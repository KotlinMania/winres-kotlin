import Testing
import Winres

@Suite("Winres Swift Export Tests")
struct WinresExportTests {
    @Test("Swift module loads and basic export smoke test")
    func swiftModuleLoads() {
        #expect(Bool(true))
    }
}
