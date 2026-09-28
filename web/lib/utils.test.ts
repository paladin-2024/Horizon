import { describe, expect, it } from "vitest";
import { authFormSchema, otpFormSchema } from "./utils";

const signUp = {
  firstName: "Caleb",
  lastName: "Nzabanita",
  address1: "Plot 4 Main Street",
  city: "Kampala",
  state: "Kampala",
  postalCode: "11101",
  dateOfBirth: "1990-01-31",
  nationalId: "CM9000000000AB",
  phone: "+256771234567",
  email: "caleb@example.com",
  password: "secret123",
};

describe("authFormSchema sign-up", () => {
  const schema = authFormSchema("sign-up");

  it("accepts a complete sign-up", () => {
    expect(schema.safeParse(signUp).success).toBe(true);
  });

  it("requires a valid phone number", () => {
    const bad = schema.safeParse({ ...signUp, phone: "0771234567" });
    expect(bad.success).toBe(false);
    expect(bad.error?.issues[0].path).toEqual(["phone"]);
    expect(bad.error?.issues[0].message).toBe("Enter a valid phone number, for example +256771234567");

    const missing: Record<string, unknown> = { ...signUp };
    delete missing.phone;
    expect(schema.safeParse(missing).success).toBe(false);
  });

  it("uses nationalId instead of ssn", () => {
    const missing: Record<string, unknown> = { ...signUp };
    delete missing.nationalId;
    const result = schema.safeParse(missing);
    expect(result.success).toBe(false);
    expect(result.error?.issues[0].path).toEqual(["nationalId"]);
    expect(Object.keys(schema.shape)).not.toContain("ssn");
  });
});

describe("authFormSchema sign-in", () => {
  const schema = authFormSchema("sign-in");

  it("only needs email and password", () => {
    expect(schema.safeParse({ email: "caleb@example.com", password: "secret123" }).success).toBe(true);
    expect(schema.safeParse({ email: "not-an-email", password: "secret123" }).success).toBe(false);
    expect(schema.safeParse({ email: "caleb@example.com", password: "123" }).success).toBe(false);
  });
});

describe("otpFormSchema", () => {
  it("needs a valid phone and a 6-digit code", () => {
    expect(otpFormSchema.safeParse({ phone: "+256771234567", code: "123456" }).success).toBe(true);
    expect(otpFormSchema.safeParse({ phone: "+256771234567", code: "12345" }).success).toBe(false);
    expect(otpFormSchema.safeParse({ phone: "+256771234567", code: "12345a" }).success).toBe(false);
    expect(otpFormSchema.safeParse({ phone: "nope", code: "123456" }).success).toBe(false);
  });
});
