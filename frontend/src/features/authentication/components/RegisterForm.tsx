import {
  AlertCircleIcon,
  LoaderCircleIcon,
  UserPlusIcon,
} from "lucide-react"
import {
  useEffect,
  useRef,
  useState,
  type ChangeEvent,
  type FormEvent,
} from "react"
import { useNavigate } from "react-router-dom"

import {
  Alert,
  AlertDescription,
  AlertTitle,
} from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Field, FieldGroup } from "@/components/ui/field"
import { registerAccount } from "@/features/authentication/api/registration-api"
import { AuthField } from "@/features/authentication/components/AuthField"
import type {
  FieldErrors,
  RegisterValues,
} from "@/features/authentication/model/auth.types"
import { validateRegistration } from "@/features/authentication/model/auth.validation"
import { ApiError } from "@/shared/api/api-error"

const INITIAL_VALUES: RegisterValues = {
  displayName: "",
  username: "",
  password: "",
  confirmPassword: "",
}

export function RegisterForm() {
  const navigate = useNavigate()
  const [values, setValues] = useState(INITIAL_VALUES)
  const [errors, setErrors] = useState<FieldErrors<RegisterValues>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isPending, setIsPending] = useState(false)
  const formRef = useRef<HTMLFormElement>(null)
  const errorFeedbackRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (submitError) {
      errorFeedbackRef.current?.focus()
    }
  }, [submitError])

  function updateField(field: keyof RegisterValues) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      const value = event.target.value
      setValues((current) => ({ ...current, [field]: value }))
      setErrors((current) => ({ ...current, [field]: undefined }))
      setSubmitError(null)
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (isPending) {
      return
    }

    const nextErrors = validateRegistration(values)
    setErrors(nextErrors)

    if (Object.keys(nextErrors).length > 0) {
      window.requestAnimationFrame(() => {
        formRef.current
          ?.querySelector<HTMLElement>('[aria-invalid="true"]')
          ?.focus()
      })
      return
    }

    setIsPending(true)
    setSubmitError(null)

    try {
      const registered = await registerAccount({
        displayName: values.displayName.trim(),
        username: values.username.trim(),
        password: values.password,
      })
      navigate("/sign-in", {
        replace: true,
        state: {
          message: `Account created for ${registered.username}. Sign in to continue.`,
        },
      })
    } catch (error) {
      if (error instanceof ApiError) {
        setErrors({
          displayName: error.fieldErrors.displayName,
          username: error.fieldErrors.username,
          password: error.fieldErrors.password,
        })
        setSubmitError(
          Object.keys(error.fieldErrors).length > 0
            ? "Correct the highlighted fields and try again."
            : error.message,
        )
      } else {
        setSubmitError("Registration is unavailable. Try again.")
      }
    } finally {
      setIsPending(false)
    }
  }

  return (
    <form
      className="flex flex-col gap-6"
      noValidate
      onSubmit={handleSubmit}
      ref={formRef}
    >
      <FieldGroup>
        <AuthField
          autoComplete="name"
          disabled={isPending}
          error={errors.displayName}
          id="register-display-name"
          label="Display name"
          name="displayName"
          onChange={updateField("displayName")}
          placeholder="e.g. Ana Jovanović"
          value={values.displayName}
        />
        <AuthField
          autoComplete="username"
          description="Use at least 3 characters. Dots, dashes, and underscores are allowed."
          disabled={isPending}
          error={errors.username}
          id="register-username"
          label="Username"
          name="username"
          onChange={updateField("username")}
          placeholder="e.g. responder.ana"
          value={values.username}
        />
        <AuthField
          autoComplete="new-password"
          description="Use at least 8 characters."
          disabled={isPending}
          error={errors.password}
          id="register-password"
          label="Password"
          name="password"
          onChange={updateField("password")}
          type="password"
          value={values.password}
        />
        <AuthField
          autoComplete="new-password"
          disabled={isPending}
          error={errors.confirmPassword}
          id="register-confirm-password"
          label="Confirm password"
          name="confirmPassword"
          onChange={updateField("confirmPassword")}
          type="password"
          value={values.confirmPassword}
        />
        <Field>
          <Button className="w-full" disabled={isPending} type="submit">
            {isPending ? (
              <LoaderCircleIcon
                className="animate-spin"
                data-icon="inline-start"
              />
            ) : (
              <UserPlusIcon data-icon="inline-start" />
            )}
            {isPending ? "Registering…" : "Register"}
          </Button>
        </Field>
      </FieldGroup>
      {submitError ? (
        <Alert ref={errorFeedbackRef} tabIndex={-1} variant="destructive">
          <AlertCircleIcon />
          <AlertTitle>Registration failed</AlertTitle>
          <AlertDescription>{submitError}</AlertDescription>
        </Alert>
      ) : null}
    </form>
  )
}
