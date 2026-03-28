package lk.ijse.examsybackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.ijse.examsybackend.dto.StudentDTO;
import lk.ijse.examsybackend.dto.TeacherDTO;
import lk.ijse.examsybackend.entity.Student;
import lk.ijse.examsybackend.entity.Teacher;
import lk.ijse.examsybackend.repository.UserAccountRepo;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserAccountRepo userAccountRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userAccountRepository.findByUsername(username)
                .map(user -> new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPasswordHash(),
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                )).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ModelMapper getModelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        // Tell STRICT mode exactly how to map the nested UserAccount ID to the flat DTO field
        modelMapper.typeMap(Student.class, StudentDTO.class).addMappings(mapper -> {
            mapper.map(src -> src.getUserAccount().getId(), StudentDTO::setUserAccountId);
        });

        //Teacher Mapping
        modelMapper.typeMap(Teacher.class, TeacherDTO.class).addMappings(mapper -> {
            mapper.map(src -> src.getUserAccount().getId(), TeacherDTO::setUserAccountId);
        });

        return modelMapper;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}