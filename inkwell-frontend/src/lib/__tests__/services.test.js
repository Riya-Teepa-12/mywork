import { describe, expect, it, jest, beforeEach } from "@jest/globals";
import { newsletterService } from "../services.js";
import { apiRequest } from "../api.js";

jest.mock("../api.js", () => ({
  apiRequest: jest.fn(),
  API_BASE: "http://localhost:8080",
}));

describe("newsletterService.getMe", () => {
  beforeEach(() => {
    apiRequest.mockReset();
  });

  it("returns null when the newsletter record is missing", async () => {
    const error = new Error("Request failed: 404");
    error.status = 404;
    apiRequest.mockRejectedValueOnce(error);

    await expect(newsletterService.getMe("token-123")).resolves.toBeNull();
    expect(apiRequest).toHaveBeenCalledWith("/newsletter/me", {
      token: "token-123",
      silentErrors: true,
    });
  });

  it("rethrows non-404 errors", async () => {
    const error = new Error("Request failed: 500");
    error.status = 500;
    apiRequest.mockRejectedValueOnce(error);

    await expect(newsletterService.getMe("token-123")).rejects.toThrow(
      "Request failed: 500"
    );
  });
});

