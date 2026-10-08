package exception;

public class AdminRoleNotAssignedException extends RuntimeException {
    public AdminRoleNotAssignedException() {
        super("This admin has no role assigned");
    }
}