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

    // ms-user-service switched to public identifier CIN: GET /users/{cin}
    @GetMapping("/users/{cin}")
    UserDTO getUserByCin(@PathVariable("cin") Long cin);

    @GetMapping("/users/me")
    UserDTO getCurrentUser();
}
