import SwiftUI
import UIKit
import shared

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController { urlString in
            guard let url = URL(string: urlString) else { return }
            DispatchQueue.main.async {
                if UIApplication.shared.canOpenURL(url) {
                    UIApplication.shared.open(url)
                } else if urlString.lowercased().hasPrefix("mailto:") {
                    // No mail app available — show an alert so the user can copy the address
                    let email = String(urlString.dropFirst(7))
                        .components(separatedBy: "?").first ?? ""
                    let alert = UIAlertController(
                        title: "No Mail App Found",
                        message: "Send us an email at:\n\(email)",
                        preferredStyle: .alert
                    )
                    alert.addAction(UIAlertAction(title: "Copy Address", style: .default) { _ in
                        UIPasteboard.general.string = email
                    })
                    alert.addAction(UIAlertAction(title: "OK", style: .cancel))
                    if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                       let root = scene.windows.first?.rootViewController {
                        root.present(alert, animated: true)
                    }
                }
            }
        }
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
