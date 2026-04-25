package tn.esprit.community.service;

import java.util.List;
import tn.esprit.community.dto.request.BlockRequest;
import tn.esprit.community.dto.response.BlockResponse;

public interface BlockService {
    BlockResponse createBlock(Long sectionId, BlockRequest blockRequest);

    BlockResponse updateBlock(Long blockId, BlockRequest blockRequest);

    List<BlockResponse> listBlocks(Long sectionId);

    void deleteBlock(Long blockId);
}
