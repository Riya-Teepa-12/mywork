import { apiRequest } from "../lib/api.js";

describe("apiRequest", () => {
  beforeEach(() => {
    jest.restoreAllMocks();
    localStorage.clear();
    global.fetch = jest.fn();
  });

  it("adds auth header from localStorage and returns parsed payload", async () => {
    localStorage.setItem("inkwell_auth", JSON.stringify({ token: "abc123" }));
    fetch.mockResolvedValue({
      ok: true,
      text: async () => JSON.stringify({ message: "ok", data: [1] }),
    });

    const result = await apiRequest("/posts");

    expect(result).toEqual({ message: "ok", data: [1] });
    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:8080/posts",
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: "Bearer abc123",
          "Content-Type": "application/json",
        }),
      })
    );
  });

  it("dispatches error event and throws on failed response", async () => {
    const dispatchSpy = jest.spyOn(window, "dispatchEvent");
    fetch.mockResolvedValue({
      ok: false,
      status: 400,
      text: async () => JSON.stringify({ message: "Bad request" }),
    });

    await expect(apiRequest("/comments")).rejects.toThrow("Bad request");
    expect(dispatchSpy).toHaveBeenCalled();
  });

  it("does not set json content-type when form data is sent", async () => {
    fetch.mockResolvedValue({
      ok: true,
      text: async () => "",
    });
    const formData = new FormData();
    formData.set("file", new Blob(["a"]), "a.txt");

    await apiRequest("/media", { method: "POST", body: formData, isFormData: true });

    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:8080/media",
      expect.objectContaining({
        headers: expect.not.objectContaining({
          "Content-Type": expect.anything(),
        }),
      })
    );
  });
});
