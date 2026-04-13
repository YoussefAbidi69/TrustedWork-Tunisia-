package tn.esprit.mscontractservicee.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import tn.esprit.mscontractservicee.dto.UserDTO;
import tn.esprit.mscontractservicee.feign.fallback.UserServiceFallback;

@FeignClient(
        name = "ms-user-service",
        url = "${user.service.url:http://localhost:8081/api}",
        fallback = UserServiceFallback.class
)
public interface UserServiceClient {

    // Older ms-user versions exposed GET /users/{cin}. Newer versions may not.
    @GetMapping("/users/{cin}")
    UserDTO getUserByCin(@PathVariable("cin") Long cin);

    // Newer ms-user versions expose KYC status by CIN (returns UserDTO) and can be used as a lookup by CIN.
    @GetMapping("/kyc/status/{cin}")
    UserDTO getUserByCinFromKycStatus(@PathVariable("cin") Long cin);

    @GetMapping("/users/me")
    UserDTO getCurrentUser();
}
