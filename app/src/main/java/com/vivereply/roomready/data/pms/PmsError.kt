package com.vivereply.roomready.data.pms

import androidx.annotation.StringRes
import com.vivereply.roomready.R

/**
 * User-facing PMS failure taxonomy. Wrong key, server down, and bad address
 * are three distinct plain-language messages; a contract mismatch gets its
 * own update prompt. Details are log-only and never contain secrets.
 */
sealed class PmsError(@StringRes val messageRes: Int) {
  data class BadAddress(val detail: String? = null) : PmsError(R.string.setup_error_bad_address)

  data object WrongKey : PmsError(R.string.setup_error_wrong_key)

  data class ServerDown(val detail: String? = null) : PmsError(R.string.setup_error_server_down)

  data class ContractMismatch(val serverVersion: Int?) :
    PmsError(R.string.setup_error_contract)

  data object BadKeyFormat : PmsError(R.string.setup_error_bad_key_format)

  data object BlankName : PmsError(R.string.setup_error_blank_name)
}

/** Thrown (or carried in a failed Result) when a PMS call cannot succeed. */
class PmsException(val error: PmsError, cause: Throwable? = null) :
  java.io.IOException(error.toString(), cause)
