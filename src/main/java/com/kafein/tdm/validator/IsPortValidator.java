package com.kafein.tdm.validator;

public class IsPortValidator {
    public boolean validate(String dbPort) {
        if ("".equals(dbPort)) {
            return false;
        } else {
            try {
                int port = Integer.parseInt(dbPort);
                return port >= 0 && port <= 65535;
            } catch (Exception e) {
                return false;
            }
        }
    }

}
