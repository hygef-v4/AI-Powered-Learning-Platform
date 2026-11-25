import { describe, expect, it } from "vitest";
import { compareTermsNewestFirst, deriveTerm } from "@/features/academics/model/term";

describe("deriveTerm", () => {

  it("returns null for missing, malformed or out-of-range dates", () => {
    for (const value of [null, undefined, "", "2027-4-3", "1999-12-31", "2100-01-01", "2027-13-01"]) {
      expect(deriveTerm(value)).toBeNull();
    }
  });
});

describe("term ordering", () => {
  it("sorts newest first with the BL5 block before its base term", () => {
    const terms = ["SP26", "FA26", "SU27", "SP27", "FA26-BL5", "SP27-BL5", "SU26-BL5"];
    expect([...terms].sort(compareTermsNewestFirst))
      .toEqual(["SU27", "SP27-BL5", "SP27", "FA26-BL5", "FA26", "SU26-BL5", "SP26"]);
  });
});
