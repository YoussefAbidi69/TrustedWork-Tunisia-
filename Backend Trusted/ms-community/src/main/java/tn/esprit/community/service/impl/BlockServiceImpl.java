package tn.esprit.community.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.community.dto.request.BlockRequest;
import tn.esprit.community.dto.response.BlockResponse;
import tn.esprit.community.entity.Block;
import tn.esprit.community.entity.enums.BlockType;
import tn.esprit.community.entity.Section;
import tn.esprit.community.exception.LearningNotFoundException;
import tn.esprit.community.repository.BlockRepository;
import tn.esprit.community.repository.SectionRepository;
import tn.esprit.community.service.BlockService;

@Service
@Transactional(readOnly = true)
public class BlockServiceImpl implements BlockService {

    private final BlockRepository blockRepository;
    private final SectionRepository sectionRepository;
    private static final String BLOCK_NOT_FOUND = "Block not found";

    public BlockServiceImpl(BlockRepository blockRepository, SectionRepository sectionRepository) {
        this.blockRepository = blockRepository;
        this.sectionRepository = sectionRepository;
    }

    @Override
    @Transactional
    public BlockResponse createBlock(Long sectionId, BlockRequest blockRequest) {
        Section section = sectionRepository
                .findById(sectionId)
                .orElseThrow(() -> new LearningNotFoundException("Section not found"));

        int orderIndex = blockRequest.getOrderIndex() != null ? blockRequest.getOrderIndex() : nextBlockOrder(sectionId);

        Block block = Block.builder()
                .section(section)
                .title(blockRequest.getTitle())
                .content(blockRequest.getContent())
                .fileUrl(blockRequest.getFileUrl())
                .orderIndex(orderIndex)
                .type(blockRequest.getType() == null ? BlockType.TEXT : blockRequest.getType())
                .build();

        return toBlockResponse(blockRepository.save(block));
    }

    @Override
    @Transactional
    public BlockResponse updateBlock(Long blockId, BlockRequest blockRequest) {
        Block block = blockRepository.findById(blockId)
            .orElseThrow(() -> new LearningNotFoundException(BLOCK_NOT_FOUND));

        if (blockRequest.getTitle() != null) {
            block.setTitle(blockRequest.getTitle());
        }
        if (blockRequest.getContent() != null) {
            block.setContent(blockRequest.getContent());
        }
        if (blockRequest.getFileUrl() != null) {
            block.setFileUrl(blockRequest.getFileUrl());
        }
        if (blockRequest.getOrderIndex() != null) {
            block.setOrderIndex(blockRequest.getOrderIndex());
        }
        if (blockRequest.getType() != null) {
            block.setType(blockRequest.getType());
        }

        return toBlockResponse(blockRepository.save(block));
    }

    @Override
    public List<BlockResponse> listBlocks(Long sectionId) {
        return blockRepository.findBySectionIdOrderByOrderIndexAsc(sectionId).stream()
                .map(this::toBlockResponse)
                .toList();
    }

    @Override
    public BlockResponse getBlock(Long blockId) {
        return blockRepository.findById(blockId)
                .map(this::toBlockResponse)
                .orElseThrow(() -> new LearningNotFoundException(BLOCK_NOT_FOUND));
    }

    @Override
    @Transactional
    public void deleteBlock(Long blockId) {
        if (!blockRepository.existsById(blockId)) {
            throw new LearningNotFoundException(BLOCK_NOT_FOUND);
        }
        blockRepository.deleteById(blockId);
    }

    private int nextBlockOrder(Long sectionId) {
        return blockRepository.findBySectionIdOrderByOrderIndexAsc(sectionId).stream()
                .mapToInt(Block::getOrderIndex)
                .max()
                .orElse(-1) + 1;
    }

    private BlockResponse toBlockResponse(Block block) {
        return BlockResponse.builder()
                .id(block.getId())
                .sectionId(block.getSection() != null ? block.getSection().getId() : null)
                .title(block.getTitle())
                .content(block.getContent())
                .fileUrl(block.getFileUrl())
                .orderIndex(block.getOrderIndex())
                .type(block.getType())
                .build();
    }
}
