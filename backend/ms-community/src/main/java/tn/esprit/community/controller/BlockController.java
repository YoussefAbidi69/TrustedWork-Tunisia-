package tn.esprit.community.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.community.dto.request.BlockRequest;
import tn.esprit.community.dto.response.BlockResponse;
import tn.esprit.community.service.BlockService;

@RestController
@RequestMapping("/api/blocks")
public class BlockController {

    private final BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    @PostMapping("/section/{sectionId}")
    public ResponseEntity<BlockResponse> createBlock(@PathVariable Long sectionId, @RequestBody BlockRequest blockRequest) {
        return new ResponseEntity<>(blockService.createBlock(sectionId, blockRequest), HttpStatus.CREATED);
    }

    @GetMapping("/section/{sectionId}")
    public ResponseEntity<List<BlockResponse>> listBlocks(@PathVariable Long sectionId) {
        return new ResponseEntity<>(blockService.listBlocks(sectionId), HttpStatus.OK);
    }

    @PutMapping("/{blockId}")
    public ResponseEntity<BlockResponse> updateBlock(@PathVariable Long blockId, @RequestBody BlockRequest blockRequest) {
        return new ResponseEntity<>(blockService.updateBlock(blockId, blockRequest), HttpStatus.OK);
    }

    @GetMapping("/{blockId}")
    public ResponseEntity<BlockResponse> getBlock(@PathVariable Long blockId) {
        return new ResponseEntity<>(blockService.getBlock(blockId), HttpStatus.OK);
    }

    @DeleteMapping("/{blockId}")
    public ResponseEntity<Void> deleteBlock(@PathVariable Long blockId) {
        blockService.deleteBlock(blockId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
