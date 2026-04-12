#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.model;

import lombok.Data;

@Data
public class MockConfig {
    private int statusCode = 200;
    private String body = "{\"status\": \"ok\"}";
    private long delayMs = 0;
}
