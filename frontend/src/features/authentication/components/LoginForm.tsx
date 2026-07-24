import { AlertCircleIcon, LoaderCircleIcon, LogInIcon } from "lucide-react"
import {
  useEffect,
  useRef,
  useState,
  type ChangeEvent,
  type FormEvent,
} from "react"
import { useLocation, useNavigate } from "react-router-dom"

import {
  Alert,
  AlertDescription,
  AlertTitle,
} from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Field, FieldGroup } from "@/components/ui/field"
import { AuthField } from "@/features/authentication/components/AuthField"
import { useAuthentication } from "@/features/authentication/context/useAuthentication"
import type {
  FieldErrors,
  LoginValues,
} from "@/features/authentication/model/auth.types"
import { validateLogin } from "@/features/authentication/model/auth.validation"
import { ApiError } from "@/shared/api/api-error"

const INITIAL_VALUES: LoginValues = {
  username: "",
  password: "",
}

export function LoginForm() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login } = useAuthentication()
  const [values, setValues] = useState(INITIAL_VALUES)
  const [errors, setErrors] = useState<FieldErrors<LoginValues>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isPending, setIsPending] = useState(false)
  const formRef = useRef<HTMLFormElement>(null)
  const errorFeedbackRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (submitError) {
      errorFeedbackRef.current?.focus()
    }
  }, [submitError])

  function updateField(field: keyof LoginValues) {
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

    const nextErrors = validateLogin(values)
    setErrors(nextErrors)

    if (Object.keys(nextErrors).length > 0) {
      requestAnimationFrame(() => {
        formRef.current
          ?.querySelector<HTMLElement>('[aria-invalid="true"]')
          ?.focus()
      })
      return
    }

    setIsPending(true)
    setSubmitError(null)

    try {
      await login({
        username: values.username.trim(),
        password: values.password,
      })
      const from =
        isRecord(location.state) && typeof location.state.from === "string"
          ? location.state.from
          : "/dashboard"
      navigate(from, { replace: true })
    } catch (error) {
      if (error instanceof ApiError) {
        setErrors({
          username: error.fieldErrors.username,
          password: error.fieldErrors.password,
        })
        setSubmitError(error.message)
      } else {
        setSubmitError(
          error instanceof Error
            ? error.message
            : "Sign-in is unavailable. Try again.",
        )
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
          autoComplete="username"
          error={errors.username}
          id="sign-in-username"
          label="Username"
          name="username"
          onChange={updateField("username")}
          placeholder="e.g. responder.ana"
          value={values.username}
        />
        <AuthField
          autoComplete="current-password"
          error={errors.password}
          id="sign-in-password"
          label="Password"
          name="password"
          onChange={updateField("password")}
          type="password"
          value={values.password}
        />
        <Field>
          <Button className="w-full" disabled={isPending} type="submit">
            {isPending ? (
              <LoaderCircleIcon
                className="animate-spin"
                data-icon="inline-start"
              />
            ) : (
              <LogInIcon data-icon="inline-start" />
            )}
            {isPending ? "Signing in…" : "Sign in"}
          </Button>
        </Field>
      </FieldGroup>
      {submitError ? (
        <Alert ref={errorFeedbackRef} tabIndex={-1} variant="destructive">
          <AlertCircleIcon />
          <AlertTitle>Sign-in failed</AlertTitle>
          <AlertDescription>{submitError}</AlertDescription>
        </Alert>
      ) : null}
    </form>
  )
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null
}
