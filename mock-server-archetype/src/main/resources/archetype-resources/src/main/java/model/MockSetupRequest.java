#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.model;

import lombok.Data;

@Data
public class MockSetupRequest {
    private String endpoint;
    private int statusCode = 200;
    private String body;
    private long delayMs = 0;
}
