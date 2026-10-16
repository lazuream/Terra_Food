// Production builds always show verification, even if a local env file is present.
export const registrationEmailVerificationEnabled = !(
  import.meta.env.DEV && import.meta.env.VITE_REGISTRATION_EMAIL_VERIFICATION === 'false'
)
