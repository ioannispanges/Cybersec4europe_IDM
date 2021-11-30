package oidc.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

@Getter
@Setter
public class Attributesreq implements container {

    @NotBlank(message = "Can't be empty")
    @Size(min = 1, message = "Must not be empty")
    private String firstname;

    @NotNull(message = "Can't be empty")
    @Size(min = 1, message = "Must not be empty")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date birthdate;

    @NotNull(message = "Can't be empty")
    @Size(min = 1, message = "Must not be empty")
    private String awardeddegree;

}
