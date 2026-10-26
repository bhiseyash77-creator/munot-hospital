package com.munot.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Getter
@Setter
@Entity
@Table(name = "hospital_settings")
public class HospitalSetting extends BaseEntity {

    @Column(name = "setting_key", nullable = false, length = 80) private String settingKey;
    @Column(name = "setting_value", columnDefinition = "TEXT") private String settingValue;
}
