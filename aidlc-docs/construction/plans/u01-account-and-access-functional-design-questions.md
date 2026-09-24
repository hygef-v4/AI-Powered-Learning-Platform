# U01 Account & Access - Functional Design Questions

Please answer each question after its `[Answer]:` tag. The current Account & Access story and service contract disagree about whether administrators can issue temporary passwords.

## Question 1 - Initial account credential

When an administrator creates or imports an account, how should the user establish the first password?

A) Keep the current service design: create the account in pending activation and email a one-time OTP; the user sets a password after verifying the OTP. Administrators never issue temporary passwords.

B) Keep the `US-IAM-007` behavior: the administrator can issue a one-time temporary password, and the user must change it at first login. Activation/reset OTP remains available where needed.

C) Support both flows, with the administrator choosing OTP activation or a forced-change temporary password for each create/import operation.

X) Other (please describe after `[Answer]:` below)

[Answer]: 
