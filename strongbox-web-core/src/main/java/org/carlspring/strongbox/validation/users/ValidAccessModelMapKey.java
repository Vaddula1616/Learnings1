package org.carlspring.strongbox.validation.users;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * @author Pablo Tirado
 */
@Documented
@Constraint(validatedBy = ValidAccessModelMapKeyValidator.class)
@Target({ ElementType.METHOD,
          ElementType.FIELD,
          ElementType.ANNOTATION_TYPE,
          ElementType.CONSTRUCTOR,
          ElementType.PARAMETER,
          ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAccessModelMapKey
{

    String message();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
