import javax.swing.*
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener


fun main() {
    // 메인 프레임 생성
    SwingUtilities.invokeLater {
        WeatherAppUI()
    }
}
class WeatherAppUI : JFrame("흑산도 철새 이동 분석") {
    private val inputField: JTextField = JTextField(20)
    private val outputArea: JTextArea = JTextArea(15, 50).apply {
        isEditable = false
    }
    private val allOutputs = mutableListOf<String>() // 누적 출력 저장용

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()

        // 입력 패널
        val inputPanel = JPanel().apply {
            layout = FlowLayout()
            add(JLabel("시간 입력 (YYYYMMDDHHMI):"))
            add(inputField)
            val submitButton = JButton("분석").apply {
                addActionListener { analyzeWeatherData() }
            }
            add(submitButton)
        }

        // 출력 패널
        val outputPanel = JScrollPane(outputArea)

        // 저장 버튼
        val saveButton = JButton("결과 저장").apply {
            addActionListener { saveResults() }
        }

        // 버튼 패널
        val buttonPanel = JPanel().apply {
            layout = FlowLayout()
            add(saveButton)
        }

        // 프레임에 추가
        add(inputPanel, BorderLayout.NORTH)
        add(outputPanel, BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)

        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }

    private fun analyzeWeatherData() {
        val inputTime = inputField.text.trim()
        if (inputTime.isBlank()) {
            JOptionPane.showMessageDialog(this, "시간을 입력하세요!", "입력 오류", JOptionPane.ERROR_MESSAGE)
            return
        }

        // TODO: JSON 파일 로드 및 WeatherData 분석 호출
        try {
            val weatherDataList = loadJson("weather_data.json")
            val output = anlzWD(inputTime, weatherDataList)
            if (output != null) {
                allOutputs.add(output)
                outputArea.append("$output\n")
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, "오류 발생: ${e.message}", "오류", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun saveResults() {
        if (allOutputs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "저장할 내용이 없습니다.", "저장 오류", JOptionPane.WARNING_MESSAGE)
            return
        }

        val fileChooser = JFileChooser().apply {
            dialogTitle = "결과 저장 위치 선택"
        }
        val result = fileChooser.showSaveDialog(this)
        if (result == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            try {
                FileManager(file.absolutePath).saveText(allOutputs.joinToString("\n\n"))
                JOptionPane.showMessageDialog(this, "결과가 저장되었습니다: ${file.absolutePath}")
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(this, "파일 저장 중 오류: ${e.message}", "저장 오류", JOptionPane.ERROR_MESSAGE)
            }
        }
    }
}