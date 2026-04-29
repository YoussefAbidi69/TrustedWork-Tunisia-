package tn.esprit.mscontractservicee.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ContractWalletIdsResponse {
    private Long contractId;
    private Long clientCin;
    private Long clientWalletCin;
    private Long freelancerCin;
    private Long freelancerWalletCin;
}
