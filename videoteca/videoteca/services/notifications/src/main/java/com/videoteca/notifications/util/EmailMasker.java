package com.videoteca.notifications.util;

/**
 * Helper para enmascarar direcciones de email en logs: deja el primer
 * carácter de la parte local + {@code ***} + el dominio
 * (p. ej. {@code pablo@videoteca.local} → {@code p***@videoteca.local}).
 */
public final class EmailMasker {

	private EmailMasker() {
	}

	/**
	 * Enmascara un email. Con {@code null} devuelve {@code "<null>"}; si no
	 * hay {@code @} se enmascara todo salvo el primer carácter.
	 */
	public static String mask(String email) {
		if (email == null) {
			return "<null>";
		}
		int at = email.lastIndexOf('@');
		String local = (at > 0) ? email.substring(0, at) : email;
		String domain = (at > 0) ? email.substring(at) : "";
		if (local.isEmpty()) {
			return "***" + domain;
		}
		return local.charAt(0) + "***" + domain;
	}

}
