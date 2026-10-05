import { act, render, screen } from "@testing-library/react";
import { LoaderProvider, useLoader } from "../context/LoaderContext.jsx";

jest.useFakeTimers();

function Probe() {
  const { isLoading, pendingCount } = useLoader();
  return (
    <div>
      <span data-testid="loading">{String(isLoading)}</span>
      <span data-testid="count">{pendingCount}</span>
    </div>
  );
}

describe("LoaderContext", () => {
  afterAll(() => {
    jest.useRealTimers();
  });

  it("tracks api start/end events", () => {
    render(
      <LoaderProvider>
        <Probe />
      </LoaderProvider>
    );

    act(() => {
      window.dispatchEvent(new CustomEvent("inkwell:api:start", { detail: { label: "Loading..." } }));
      jest.advanceTimersByTime(200);
    });
    expect(screen.getByTestId("count")).toHaveTextContent("1");
    expect(screen.getByTestId("loading")).toHaveTextContent("true");

    act(() => {
      window.dispatchEvent(new CustomEvent("inkwell:api:end", { detail: {} }));
      jest.advanceTimersByTime(200);
    });
    expect(screen.getByTestId("count")).toHaveTextContent("0");
  });

  it("ignores events with trackLoader false", () => {
    render(
      <LoaderProvider>
        <Probe />
      </LoaderProvider>
    );

    act(() => {
      window.dispatchEvent(new CustomEvent("inkwell:api:start", { detail: { trackLoader: false } }));
      jest.advanceTimersByTime(200);
    });
    expect(screen.getByTestId("count")).toHaveTextContent("0");
  });
});
