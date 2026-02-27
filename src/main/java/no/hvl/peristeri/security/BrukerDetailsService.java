package no.hvl.peristeri.security;

import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.bruker.BrukerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class BrukerDetailsService implements UserDetailsService {
	private final Logger logger = LoggerFactory.getLogger(BrukerDetailsService.class);

	private final BrukerRepository brukerRepository;


	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return brukerRepository.findByEpost(username)
		                                  .orElseThrow(() -> new UsernameNotFoundException("User not found"));

	}
}
