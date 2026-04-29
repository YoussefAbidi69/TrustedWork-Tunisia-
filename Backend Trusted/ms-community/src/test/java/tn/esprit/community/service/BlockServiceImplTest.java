package tn.esprit.community.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.community.dto.request.BlockRequest;
import tn.esprit.community.dto.response.BlockResponse;
import tn.esprit.community.entity.Block;
import tn.esprit.community.entity.Section;
import tn.esprit.community.entity.enums.BlockType;
import tn.esprit.community.exception.LearningNotFoundException;
import tn.esprit.community.repository.BlockRepository;
import tn.esprit.community.repository.SectionRepository;
import tn.esprit.community.service.impl.BlockServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockServiceImplTest {

    @Mock private BlockRepository blockRepository;
    @Mock private SectionRepository sectionRepository;

    @InjectMocks
    private BlockServiceImpl blockService;

    @Test
    @DisplayName("shouldCreateBlockWithNextOrder_whenOrderIndexNotProvided")
    void shouldCreateBlockWithNextOrder_whenOrderIndexNotProvided() {
        Section section = Section.builder().id(1L).build();
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));

        Block existingBlock = Block.builder().orderIndex(5).build();
        when(blockRepository.findBySectionIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(existingBlock));

        Block savedBlock = Block.builder()
                .id(10L)
                .section(section)
                .title("New Block")
                .type(BlockType.TEXT)
                .orderIndex(6)
                .build();
        when(blockRepository.save(any(Block.class))).thenReturn(savedBlock);

        BlockRequest request = BlockRequest.builder().title("New Block").build();
        BlockResponse response = blockService.createBlock(1L, request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getOrderIndex()).isEqualTo(6);
        assertThat(response.getType()).isEqualTo(BlockType.TEXT);
    }

    @Test
    @DisplayName("shouldThrowLearningNotFoundException_whenSectionNotFoundOnCreate")
    void shouldThrowLearningNotFoundException_whenSectionNotFoundOnCreate() {
        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blockService.createBlock(99L, BlockRequest.builder().build()))
                .isInstanceOf(LearningNotFoundException.class)
                .hasMessageContaining("Section not found");
    }

    @Test
    @DisplayName("shouldUpdateBlockFields_whenProvidedInRequest")
    void shouldUpdateBlockFields_whenProvidedInRequest() {
        Block existing = Block.builder().id(2L).title("Old").content("Old content").build();
        when(blockRepository.findById(2L)).thenReturn(Optional.of(existing));

        Block updated = Block.builder().id(2L).title("New").content("New content").type(BlockType.VIDEO).orderIndex(2).fileUrl("url").build();
        when(blockRepository.save(existing)).thenReturn(updated);

        BlockRequest request = BlockRequest.builder()
                .title("New").content("New content").type(BlockType.VIDEO).orderIndex(2).fileUrl("url")
                .build();
        BlockResponse response = blockService.updateBlock(2L, request);

        assertThat(response.getTitle()).isEqualTo("New");
        verify(blockRepository).save(existing);
    }

    @Test
    @DisplayName("shouldListBlocksBySection")
    void shouldListBlocksBySection() {
        Block b = Block.builder().id(1L).title("A").build();
        when(blockRepository.findBySectionIdOrderByOrderIndexAsc(3L)).thenReturn(List.of(b));

        List<BlockResponse> responses = blockService.listBlocks(3L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("shouldGetBlockById")
    void shouldGetBlockById() {
        Block b = Block.builder().id(1L).title("A").build();
        when(blockRepository.findById(1L)).thenReturn(Optional.of(b));

        BlockResponse response = blockService.getBlock(1L);

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("shouldDeleteBlock")
    void shouldDeleteBlock() {
        when(blockRepository.existsById(5L)).thenReturn(true);
        blockService.deleteBlock(5L);
        verify(blockRepository).deleteById(5L);
    }

    @Test
    @DisplayName("shouldThrowException_whenDeletingNonExistentBlock")
    void shouldThrowException_whenDeletingNonExistentBlock() {
        when(blockRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> blockService.deleteBlock(99L))
                .isInstanceOf(LearningNotFoundException.class);
    }
}
