import { describe, expect, it } from "vitest";
import { compareTermsNewestFirst, isTerm, deriveTerm, listTermOptions } from "@/features/academics/model/term";

describe("deriveTerm", () => {
  it.each([
    ["2027-01-01", "SP27"], ["2027-02-15", "SP27"], ["2027-03-31", "SP27"], ["2027-04-03", "SP27-BL5"],
    ["2027-05-01", "SU27"], ["2027-06-30", "SU27"], ["2027-07-10", "SU27"], ["2027-08-31", "SU27-BL5"],
    ["2026-09-01", "FA26"], ["2026-10-20", "FA26"], ["2026-11-14", "FA26"], ["2026-12-31", "FA26-BL5"],
    ["2000-01-01", "SP00"], ["2099-12-31", "FA99-BL5"], ["2009-04-30", "SP09-BL5"],
  ])("maps %s to %s", (date, term) => {
    expect(deriveTerm(date)).toBe(term);
  });

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

  it("builds distinct options from loaded classes and ignores malformed values", () => {
    expect(listTermOptions([{ term: "SP27" }, { term: "FA26" }, { term: "SP27" }, { term: null }, { term: "2026-1" }]))
      .toEqual(["SP27", "FA26"]);
    expect(isTerm("SP27-BL5")).toBe(true);
    expect(isTerm("sp27")).toBe(false);
  });
});
