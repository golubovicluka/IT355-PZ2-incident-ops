import type {
  FieldErrors,
  LoginValues,
  RegisterValues,
} from "./auth.types"

const USERNAME_PATTERN = /^[a-zA-Z0-9._-]+$/

function validateUsername(username: string) {
  const value = username.trim()

  if (!value) {
    return "Enter your username."
  }

  if (value.length < 3) {
    return "Username must contain at least 3 characters."
  }

  if (!USERNAME_PATTERN.test(value)) {
    return "Use letters, numbers, dots, dashes, or underscores only."
  }
}

function validatePassword(password: string) {
  if (!password) {
    return "Enter your password."
  }

  if (password.length < 8) {
    return "Password must contain at least 8 characters."
  }
}

export function validateLogin(values: LoginValues) {
  const errors: FieldErrors<LoginValues> = {}
  const usernameError = validateUsername(values.username)
  const passwordError = validatePassword(values.password)

  if (usernameError) {
    errors.username = usernameError
  }

  if (passwordError) {
    errors.password = passwordError
  }

  return errors
}

export function validateRegistration(values: RegisterValues) {
  const errors: FieldErrors<RegisterValues> = {}

  if (values.displayName.trim().length < 2) {
    errors.displayName = "Enter the name your team will recognize."
  }

  const usernameError = validateUsername(values.username)
  const passwordError = validatePassword(values.password)

  if (usernameError) {
    errors.username = usernameError
  }

  if (passwordError) {
    errors.password = passwordError
  }

  if (!values.confirmPassword) {
    errors.confirmPassword = "Confirm your password."
  } else if (values.confirmPassword !== values.password) {
    errors.confirmPassword = "Passwords do not match."
  }

  return errors
}
