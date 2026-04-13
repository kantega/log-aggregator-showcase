package ${package}.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Provider-specific payload shape for ${providerName}.
 *
 * TODO(skill): Fill in fields based on the ${providerName} OpenAPI spec's primary
 * request schema. Add nested types (e.g. Document, Attachment) as inner static
 * classes. Keep the class Lombok-@Data so Jackson can serialize it. Re-add
 * @AllArgsConstructor once fields exist.
 */
@Data
@NoArgsConstructor
public class ${providerName}Payload {
    // Intentionally empty — skill fills in from the OpenAPI spec.
}
