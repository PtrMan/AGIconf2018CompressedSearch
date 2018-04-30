package misc;

class InternalError extends Exception {
	String msg;

	public InternalError(String msg) {
		this.msg = msg;
	}

	@Override
    public String getMessage() {
        return "Internal Error - " + msg;
    }
}
