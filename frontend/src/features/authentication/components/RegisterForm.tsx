import { InfoIcon, UserPlusIcon } from "lucide-react"
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
  RegisterValues,
} from "@/features/authentication/model/auth.types"
import { validateRegistration } from "@/features/authentication/model/auth.validation"

const INITIAL_VALUES: RegisterValues = {
  displayName: "",
  username: "",
  password: "",
  confirmPassword: "",
}

export function RegisterForm() {
  const [values, setValues] = useState(INITIAL_VALUES)
  const [errors, setErrors] = useState<FieldErrors<RegisterValues>>({})
  const [isValidated, setIsValidated] = useState(false)

  function updateField(field: keyof RegisterValues) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      const value = event.target.value
      setValues((current) => ({ ...current, [field]: value }))
      setErrors((current) => ({ ...current, [field]: undefined }))
      setIsValidated(false)
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const nextErrors = validateRegistration(values)
    setErrors(nextErrors)
    setIsValidated(Object.keys(nextErrors).length === 0)
  }

  return (
    <form className="flex flex-col gap-6" noValidate onSubmit={handleSubmit}>
      <FieldGroup>
        <AuthField
          autoComplete="name"
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
          error={errors.confirmPassword}
          id="register-confirm-password"
          label="Confirm password"
          name="confirmPassword"
          onChange={updateField("confirmPassword")}
          type="password"
          value={values.confirmPassword}
        />
        <Field>
          <Button className="w-full" type="submit">
            <UserPlusIcon data-icon="inline-start" />
            Validate details
          </Button>
        </Field>
      </FieldGroup>
      {isValidated ? (
        <Alert>
          <InfoIcon />
          <AlertTitle>Details are valid</AlertTitle>
          <AlertDescription>
            No account was created. Account creation, initial role, team
            assignment, and approval behavior still require a backend decision.
          </AlertDescription>
        </Alert>
      ) : null}
    </form>
  )
}
