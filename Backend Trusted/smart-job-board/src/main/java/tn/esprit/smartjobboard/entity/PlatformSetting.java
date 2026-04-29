package tn.esprit.smartjobboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Key-value platform configuration (e.g. mock average budget) persisted for admin visibility.
 */
@Entity
@Table(name = "platform_settings")
@Getter
@Setter
public class PlatformSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String settingKey;

    @Column(nullable = false, length = 512)
    private String settingValue;
}
