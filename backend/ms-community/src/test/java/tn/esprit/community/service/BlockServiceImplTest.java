package tn.esprit.community.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.community.dto.request.BlockRequest;
import tn.esprit.community.dto.response.BlockResponse;
import tn.esprit.community.entity.Block;
import tn.esprit.community.entity.Enum.BlockType;
import tn.esprit.community.entity.Section;
import tn.esprit.community.exception.LearningNotFoundException;
import tn.esprit.community.repository.BlockRepository;
import tn.esprit.community.repository.SectionRepository;
import tn.esprit.community.service.impl.BlockServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlockServiceImplTest {

    @Mock private BlockRepository blockRepository;
    @Mock private SectionRepository sectionRepository;

    @InjectMocks
    private BlockServiceImpl blockService;

    @Test
    @DisplayName("shouldThrowLearningNotFoundException_whenSectionMissing")
    void shouldThrowLearningNotFoundException_whenSectionMissing() {
        when(sectionRepository.findById(55L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blockService.createBlock(55L, BlockRequest.builder().title("T").build()))
                .isInstanceOf(LearningNotFoundException.class)
                .hasMessageContaining("Section not found");
    }

    @Test
    @DisplayName("shouldDefaultOrderAndType_whenOrderIndexAndTypeMissing")
    void shouldDefaultOrderAndType_whenOrderIndexAndTypeMissing() {
        Section section = Section.builder().id(2L).build();
        when(sectionRepository.findById(2L)).thenReturn(Optional.of(section));
        when(blockRepository.findBySection_IdOrderByOrderIndexAsc(2L)).thenReturn(List.of(
                Block.builder().orderIndex(0).build(),
                Block.builder().orderIndex(1).build()
        ));
        when(blockRepository.save(any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BlockResponse response = blockService.createBlock(2L, BlockRequest.builder().title("Intro").build());

        ArgumentCaptor<Block> captor = ArgumentCaptor.forClass(Block.class);
        verify(blockRepository).save(captor.capture());
        Block saved = captor.getValue();

        assertThat(saved.getOrderIndex()).isEqualTo(2);
        assertThat(saved.getType()).isEqualTo(BlockType.TEXT);
        assertThat(response.getOrderIndex()).isEqualTo(2);
        assertThat(response.getType()).isEqualTo(BlockType.TEXT);
    }

    @Test
    @DisplayName("shouldUpdateFields_whenRequestProvidesValues")
    void shouldUpdateFields_whenRequestProvidesValues() {
        Block existing = Block.builder().id(1L).title("Old").content("C").fileUrl("u").orderIndex(1).type(BlockType.TEXT).build();
        when(blockRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(blockRepository.save(existing)).thenReturn(existing);

        BlockRequest update = BlockRequest.builder()
                .title("New")
                .content("Updated")
                .orderIndex(5)
                .type(BlockType.VIDEO)
                .build();

        BlockResponse response = blockService.updateBlock(1L, update);

        assertThat(response.getTitle()).isEqualTo("New");
        assertThat(response.getContent()).isEqualTo("Updated");
        assertThat(response.getOrderIndex()).isEqualTo(5);
        assertThat(response.getType()).isEqualTo(BlockType.VIDEO);
    }

    @Test
    @DisplayName("shouldDeleteBlock_whenExists")
    void shouldDeleteBlock_whenExists() {
        when(blockRepository.existsById(10L)).thenReturn(true);

        blockService.deleteBlock(10L);

        verify(blockRepository).deleteById(10L);
    }

    @Test
    @DisplayName("shouldThrowLearningNotFoundException_whenDeleteMissing")
    void shouldThrowLearningNotFoundException_whenDeleteMissing() {
        when(blockRepository.existsById(10L)).thenReturn(false);

        assertThatThrownBy(() -> blockService.deleteBlock(10L))
                .isInstanceOf(LearningNotFoundException.class)
                .hasMessageContaining("Block not found");
    }
}
