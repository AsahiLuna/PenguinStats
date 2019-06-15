package io.penguinstats.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class CustomizedBadRequestException extends WebApplicationException {

	private static final long serialVersionUID = 1L;

	public CustomizedBadRequestException(String message) {
		super(Response.status(Response.Status.UNAUTHORIZED).entity(message).type(MediaType.TEXT_PLAIN).build());
	}

}
