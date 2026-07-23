import { InfoIcon, LogInIcon } from "lucide-react"
import { useState, type ChangeEvent, type FormEvent } from "react"

import {
  Alert,
  AlertDescription,
  AlertTitle,
} from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Field, FieldGroup } from "@/components/ui/field"
import { AuthField } from "@/features/authentication/components/AuthField"
import type {
  FieldErrors,
  LoginValues,
} from "@/features/authentication/model/auth.types"
import { validateLogin } from "@/features/authentication/model/auth.validation"

const INITIAL_VALUES: LoginValues = {
  username: "",
  password: "",
}

export function LoginForm() {
  const [values, setValues] = useState(INITIAL_VALUES)
  const [errors, setErrors] = useState<FieldErrors<LoginValues>>({})
  const [isValidated, setIsValidated] = useState(false)

  function updateField(field: keyof LoginValues) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      const value = event.target.value
      setValues((current) => ({ ...current, [field]: value }))
      setErrors((current) => ({ ...current, [field]: undefined }))
      setIsValidated(false)
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const nextErrors = validateLogin(values)
    setErrors(nextErrors)
    setIsValidated(Object.keys(nextErrors).length === 0)
  }

  return (
    <form className="flex flex-col gap-6" noValidate onSubmit={handleSubmit}>
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
          <Button className="w-full" type="submit">
            <LogInIcon data-icon="inline-start" />
            Sign in
          </Button>
        </Field>
      </FieldGroup>
      {isValidated ? (
        <Alert>
          <InfoIcon />
          <AlertTitle>Sign-in service is not connected</AlertTitle>
          <AlertDescription>
            The form is valid. Server sign-in will use the planned
            <code> POST /login</code> endpoint when backend auth is available.
          </AlertDescription>
        </Alert>
      ) : null}
    </form>
  )
}
