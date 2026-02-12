package org.example.splitpalbackendv2.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration


@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class Configuration