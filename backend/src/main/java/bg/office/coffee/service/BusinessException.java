package bg.office.coffee.service;

/** Нарушено бизнес правило – връща се като 400 с четимо съобщение. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
