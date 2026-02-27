package no.hvl.peristeri.config;

import org.h2.tools.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.sql.SQLException;

/**
 * kun for å kunne se databasen i intellij inspector<br>
 * Denne er kun aktiv når "dev" profilen er aktivert.
 */
@Configuration
@Profile("dev")
public class H2Config {

	@Bean(initMethod = "start", destroyMethod = "stop")
	public Server h2Server() throws SQLException {
		return Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092");
	}
}
