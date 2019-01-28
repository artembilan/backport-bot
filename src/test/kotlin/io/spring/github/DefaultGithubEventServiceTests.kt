/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.github

import com.nhaarman.mockitokotlin2.*
import io.spring.github.api.*
import io.spring.github.event.BackportService
import io.spring.github.event.DefaultGithubEventService
import org.assertj.core.api.Assertions.*
import org.junit.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

/**
 * @author Rob Winch
 */
class DefaultGithubEventServiceTests {
    val backports : BackportService = mock()
    val events = DefaultGithubEventService(backports)

    @Test
    fun backportIssueWhenFindBranchNameByLabelNameEmptyThenFalse() {
        whenever(backports.findBranchNameByLabelName(any())).thenReturn(Mono.empty())

        val issue = Issue(1, "Title", null, listOf())
        val issueEvent = IssueEvent("labeled", IssueEvent.Repository("rwinch/test"), IssueEvent.Sender("rwinch"), IssueEvent.Label("Label"), issue)

        StepVerifier.create(events.backport(issueEvent))
                .expectNext(false)
                .verifyComplete()
    }

    @Test
    fun backportIssueWhenActionOtherThenNoLabelRemovedAndFalse() {
        whenever(backports.findBranchNameByLabelName(any())).thenReturn(Mono.just("1.0.x"))

        val issue = Issue(1, "Title", null, listOf())
        val issueEvent = IssueEvent("other", IssueEvent.Repository("rwinch/test"), IssueEvent.Sender("rwinch"), IssueEvent.Label("Label"), issue)

        StepVerifier.create(events.backport(issueEvent))
                .expectNext(false)
                .verifyComplete()
    }

    @Test
    fun backportIssueWhenIssueForMilestoneThenLabelRemovedAndFalse() {
        whenever(backports.findBranchNameByLabelName(any())).thenReturn(Mono.just("1.0.x"))
        whenever(backports.removeLabel(any(), any())).thenReturn(Mono.empty())
        whenever(backports.findMilestoneNumber(any())).thenReturn(Mono.just(1))
        whenever(backports.isIssueForMilestone(any(), any())).thenReturn(Mono.just(true))

        val issue = Issue(1, "Title", null, listOf())
        val issueEvent = IssueEvent("labeled", IssueEvent.Repository("rwinch/test"), IssueEvent.Sender("rwinch"), IssueEvent.Label("Label"), issue)

        StepVerifier.create(events.backport(issueEvent))
                .expectNext(false)
                .verifyComplete()

        val issueCaptor = argumentCaptor<IssueRef>()
        val labelCaptor = argumentCaptor<String>()

        verify(backports).removeLabel(issueCaptor.capture(), labelCaptor.capture())

        assertThat(labelCaptor.firstValue).isEqualTo(issueEvent.label?.name)
        assertThat(issueCaptor.firstValue).isEqualTo(IssueRef(RepositoryRef(issueEvent.repository.fullName), issue.number))
    }

    @Test
    fun backportIssueWhenIssueForMilestoneThenTrue() {
        whenever(backports.findBranchNameByLabelName(any())).thenReturn(Mono.just("1.0.x"))
        whenever(backports.removeLabel(any(), any())).thenReturn(Mono.empty())
        whenever(backports.findMilestoneNumber(any())).thenReturn(Mono.just(1))
        whenever(backports.isIssueForMilestone(any(), any())).thenReturn(Mono.just(true))

        val issue = Issue(1, "Title", null, listOf())
        val issueEvent = IssueEvent("labeled", IssueEvent.Repository("rwinch/test"), IssueEvent.Sender("rwinch"), IssueEvent.Label("Label"), issue)

        StepVerifier.create(events.backport(issueEvent))
                .expectNext(false)
                .verifyComplete()
    }

    @Test
    fun backportIssueWhenNotIssueForMilestoneThenTrue() {
        whenever(backports.findBranchNameByLabelName(any())).thenReturn(Mono.just("1.0.x"))
        whenever(backports.removeLabel(any(), any())).thenReturn(Mono.empty())
        whenever(backports.findMilestoneNumber(any())).thenReturn(Mono.just(1))
        whenever(backports.isIssueForMilestone(any(), any())).thenReturn(Mono.just(false))
        whenever(backports.createBackport(any(), any(), any())).thenReturn(Mono.just(100))

        val issue = Issue(1, "Title", null, listOf())
        val issueEvent = IssueEvent("labeled", IssueEvent.Repository("rwinch/test"), IssueEvent.Sender("rwinch"), IssueEvent.Label("Label"), issue)

        StepVerifier.create(events.backport(issueEvent))
                .expectNext(true)
                .verifyComplete()

        verify(backports).createBackport(any(), any(), any())
    }
}