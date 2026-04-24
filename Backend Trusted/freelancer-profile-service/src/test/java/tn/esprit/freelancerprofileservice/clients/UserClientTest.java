package tn.esprit.freelancerprofileservice.clients;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserClientTest {

    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private UserClient userClient;

    private UserClient.PublicUserResponse buildUser(String firstName, String lastName,
                                                    String email, String phone) {
        UserClient.PublicUserResponse u = new UserClient.PublicUserResponse();
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setEmail(email);
        u.setPhone(phone);
        return u;
    }

    // ─── getPublicUser ───────────────────────────────────────────────────────

    @Test
    void getPublicUser_shouldReturnUser_whenIdentityEndpointResponds() {
        UserClient.PublicUserResponse expected = buildUser("Ahmed", "Ben Ali", "ahmed@test.com", "+21612345678");

        when(restTemplate.getForObject(anyString(), eq(UserClient.PublicUserResponse.class)))
                .thenReturn(expected);

        UserClient.PublicUserResponse result = userClient.getPublicUser(1L);

        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Ahmed");
        assertThat(result.getEmail()).isEqualTo("ahmed@test.com");
    }

    @Test
    void getPublicUser_shouldFallbackToLegacy_whenIdentityReturnsNull() {
        UserClient.PublicUserResponse legacyUser = buildUser("Sami", "Trabelsi", "sami@test.com", null);

        when(restTemplate.getForObject(anyString(), eq(UserClient.PublicUserResponse.class)))
                .thenReturn(null)
                .thenReturn(legacyUser);

        UserClient.PublicUserResponse result = userClient.getPublicUser(1L);

        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Sami");
    }

    @Test
    void getPublicUser_shouldReturnNull_whenRestTemplateThrows() {
        when(restTemplate.getForObject(anyString(), eq(UserClient.PublicUserResponse.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        UserClient.PublicUserResponse result = userClient.getPublicUser(1L);

        assertThat(result).isNull();
    }

    // ─── getUserFullName ─────────────────────────────────────────────────────

    @Test
    void getUserFullName_shouldReturnFullName() {
        when(restTemplate.getForObject(anyString(), eq(UserClient.PublicUserResponse.class)))
                .thenReturn(buildUser("Rania", "Jrad", "rania@test.com", null));

        String name = userClient.getUserFullName(1L);

        assertThat(name).isEqualTo("Rania Jrad");
    }

    @Test
    void getUserFullName_shouldReturnUnknown_whenUserNull() {
        when(restTemplate.getForObject(anyString(), eq(UserClient.PublicUserResponse.class)))
                .thenThrow(new RuntimeException("service down"));

        String name = userClient.getUserFullName(1L);

        assertThat(name).isEqualTo("Unknown User");
    }

    @Test
    void getUserFullName_shouldReturnUnknown_whenNamesAreBlank() {
        when(restTemplate.getForObject(anyString(), eq(UserClient.PublicUserResponse.class)))
                .thenReturn(buildUser("  ", "  ", "u@test.com", null));

        String name = userClient.getUserFullName(1L);

        assertThat(name).isEqualTo("Unknown User");
    }

    @Test
    void getUserFullName_shouldHandleNullNames() {
        when(restTemplate.getForObject(anyString(), eq(UserClient.PublicUserResponse.class)))
                .thenReturn(buildUser(null, null, "u@test.com", null));

        String name = userClient.getUserFullName(1L);

        assertThat(name).isEqualTo("Unknown User");
    }

    // ─── getUserEmail ────────────────────────────────────────────────────────

    @Test
    void getUserEmail_shouldReturnEmail() {
        when(restTemplate.getForObject(anyString(), eq(UserClient.PublicUserResponse.class)))
                .thenReturn(buildUser("Ahmed", "Ali", "ahmed@test.com", null));

        String email = userClient.getUserEmail(1L);

        assertThat(email).isEqualTo("ahmed@test.com");
    }

    @Test
    void getUserEmail_shouldReturnNull_whenUserNull() {
        when(restTemplate.getForObject(anyString(), eq(UserClient.PublicUserResponse.class)))
                .thenThrow(new RuntimeException("down"));

        String email = userClient.getUserEmail(1L);

        assertThat(email).isNull();
    }

    @Test
    void getUserEmail_shouldReturnNull_whenEmailBlank() {
        when(restTemplate.getForObject(anyString(), eq(UserClient.PublicUserResponse.class)))
                .thenReturn(buildUser("Ahmed", "Ali", "   ", null));

        String email = userClient.getUserEmail(1L);

        assertThat(email).isNull();
    }

    // ─── getUserPhone ────────────────────────────────────────────────────────

    @Test
    void getUserPhone_shouldReturnPhone() {
        when(restTemplate.getForObject(anyString(), eq(UserClient.PublicUserResponse.class)))
                .thenReturn(buildUser("Ahmed", "Ali", "a@test.com", "+21612345678"));

        String phone = userClient.getUserPhone(1L);

        assertThat(phone).isEqualTo("+21612345678");
    }

    @Test
    void getUserPhone_shouldReturnNull_whenPhoneNull() {
        when(restTemplate.getForObject(anyString(), eq(UserClient.PublicUserResponse.class)))
                .thenReturn(buildUser("Ahmed", "Ali", "a@test.com", null));

        String phone = userClient.getUserPhone(1L);

        assertThat(phone).isNull();
    }

    @Test
    void getUserPhone_shouldReturnNull_whenPhoneBlank() {
        when(restTemplate.getForObject(anyString(), eq(UserClient.PublicUserResponse.class)))
                .thenReturn(buildUser("Ahmed", "Ali", "a@test.com", "   "));

        String phone = userClient.getUserPhone(1L);

        assertThat(phone).isNull();
    }
}
