package com.psa.capstone.be;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.psa.capstone.be.entity.BranchSummaryEntity;
import com.psa.capstone.be.repository.BranchSummaryRepository;
import com.psa.capstone.be.service.BranchSummaryService;

@ExtendWith(MockitoExtension.class)
class BranchSummaryServiceTest {

    @Mock
    private BranchSummaryRepository branchSummaryRepository;

    @InjectMocks
    private BranchSummaryService branchSummaryService;

    @Test
    void trackBranchCreatesNewSummaryWhenComparisonIsNotTracked() {
        when(branchSummaryRepository.findByProjectIdAndBranchNameAndComparedBranch(1L, "feature", "main"))
                .thenReturn(Optional.empty());
        when(branchSummaryRepository.save(any(BranchSummaryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BranchSummaryEntity result = branchSummaryService.trackBranch(1L, "feature", "commit-a", "main");

        ArgumentCaptor<BranchSummaryEntity> captor = ArgumentCaptor.forClass(BranchSummaryEntity.class);
        verify(branchSummaryRepository).save(captor.capture());

        BranchSummaryEntity saved = captor.getValue();
        assertEquals(1L, saved.getProjectId());
        assertEquals("feature", saved.getBranchName());
        assertEquals("main", saved.getComparedBranch());
        assertEquals("commit-a", saved.getLastCommitId());
        assertTrue(saved.isNeedsUpdate());
        assertSame(saved, result);
    }

    @Test
    void trackBranchDoesNotSaveWhenCommitAndSummaryAreUnchanged() {
        BranchSummaryEntity existing = summary("commit-a", "detailed summary", false);
        when(branchSummaryRepository.findByProjectIdAndBranchNameAndComparedBranch(1L, "feature", "main"))
                .thenReturn(Optional.of(existing));

        BranchSummaryEntity result = branchSummaryService.trackBranch(1L, "feature", "commit-a", "main");

        assertSame(existing, result);
        verify(branchSummaryRepository, never()).save(any());
    }

    @Test
    void trackBranchMarksSummaryForUpdateWhenCommitChanges() {
        BranchSummaryEntity existing = summary("commit-a", "detailed summary", false);
        when(branchSummaryRepository.findByProjectIdAndBranchNameAndComparedBranch(1L, "feature", "main"))
                .thenReturn(Optional.of(existing));
        when(branchSummaryRepository.save(existing)).thenReturn(existing);

        BranchSummaryEntity result = branchSummaryService.trackBranch(1L, "feature", "commit-b", "main");

        assertSame(existing, result);
        assertEquals("commit-b", result.getLastCommitId());
        assertTrue(result.isNeedsUpdate());
        verify(branchSummaryRepository).save(existing);
    }

    @Test
    void saveSummaryUpdatesExistingSummaryAndMarksItFresh() {
        BranchSummaryEntity existing = summary("commit-a", "old", true);
        when(branchSummaryRepository.findByProjectIdAndBranchNameAndComparedBranch(1L, "feature", "main"))
                .thenReturn(Optional.of(existing));
        when(branchSummaryRepository.save(existing)).thenReturn(existing);

        BranchSummaryEntity result = branchSummaryService.saveSummary(
                1L, "feature", "main", "new detailed", "new concise", "commit-b");

        assertSame(existing, result);
        assertEquals("new detailed", result.getDetailedSummary());
        assertEquals("new concise", result.getConciseSummary());
        assertEquals("commit-b", result.getLastCommitId());
        assertFalse(result.isNeedsUpdate());
        verify(branchSummaryRepository).save(existing);
    }

    private static BranchSummaryEntity summary(String commitId, String detailedSummary, boolean needsUpdate) {
        BranchSummaryEntity entity = new BranchSummaryEntity();
        entity.setProjectId(1L);
        entity.setBranchName("feature");
        entity.setComparedBranch("main");
        entity.setLastCommitId(commitId);
        entity.setDetailedSummary(detailedSummary);
        entity.setNeedsUpdate(needsUpdate);
        return entity;
    }
}
