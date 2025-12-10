package com.example.projectantrianrsrjkelompok2.model

data class DiagnosisItem(
    val id: String = "",
    val name: String = "",
    val description: String = ""
)

data class MedicineItem(
    val id: String = "",
    val name: String = "",
    val dosage: String = "",
    val frequency: String = ""
) {
    fun getFullDescription(): String {
        return "$name $dosage ($frequency)"
    }
}

// ✅ Data Master Diagnosis
object DiagnosisData {
    val diagnosisList = listOf(
        DiagnosisItem("D001", "Demam Tifoid", "Infeksi bakteri Salmonella typhi"),
        DiagnosisItem("D002", "Influenza", "Infeksi virus influenza"),
        DiagnosisItem("D003", "ISPA", "Infeksi Saluran Pernapasan Atas"),
        DiagnosisItem("D004", "Gastritis", "Peradangan lambung"),
        DiagnosisItem("D005", "Hipertensi", "Tekanan darah tinggi"),
        DiagnosisItem("D006", "Diabetes Mellitus", "Penyakit gula darah"),
        DiagnosisItem("D007", "Diare", "Gangguan pencernaan"),
        DiagnosisItem("D008", "Asma", "Gangguan pernapasan"),
        DiagnosisItem("D009", "Migrain", "Sakit kepala hebat"),
        DiagnosisItem("D010", "Vertigo", "Pusing berputar"),
        DiagnosisItem("D011", "Demam Berdarah", "Infeksi virus dengue"),
        DiagnosisItem("D012", "Malaria", "Infeksi parasit plasmodium"),
        DiagnosisItem("D013", "Anemia", "Kekurangan sel darah merah"),
        DiagnosisItem("D014", "Alergi", "Reaksi hipersensitivitas"),
        DiagnosisItem("D015", "Faringitis", "Radang tenggorokan")
    )

    fun getDiagnosisNames(): List<String> {
        return diagnosisList.map { it.name }
    }
}

// ✅ Data Master Obat
object MedicineData {
    val medicineList = listOf(
        MedicineItem("M001", "Paracetamol", "500mg", "3x1"),
        MedicineItem("M002", "Amoxicillin", "500mg", "3x1"),
        MedicineItem("M003", "Vitamin C", "1000mg", "1x1"),
        MedicineItem("M004", "Antasida", "500mg", "3x1"),
        MedicineItem("M005", "Ibuprofen", "400mg", "3x1"),
        MedicineItem("M006", "Omeprazole", "20mg", "2x1"),
        MedicineItem("M007", "Cetirizine", "10mg", "1x1"),
        MedicineItem("M008", "Salbutamol", "4mg", "3x1"),
        MedicineItem("M009", "Metformin", "500mg", "2x1"),
        MedicineItem("M010", "Amlodipine", "5mg", "1x1"),
        MedicineItem("M011", "Codeine", "15mg", "3x1"),
        MedicineItem("M012", "Loperamide", "2mg", "2x1"),
        MedicineItem("M013", "Ranitidine", "150mg", "2x1"),
        MedicineItem("M014", "Dexamethasone", "0.5mg", "3x1"),
        MedicineItem("M015", "Chlorpheniramine", "4mg", "3x1"),
        MedicineItem("M016", "Vitamin B Complex", "-", "1x1"),
        MedicineItem("M017", "Asam Mefenamat", "500mg", "3x1"),
        MedicineItem("M018", "Antimo", "50mg", "3x1"),
        MedicineItem("M019", "OBH Combi", "15ml", "3x1"),
        MedicineItem("M020", "Betahistine", "6mg", "3x1")
    )

    fun getMedicineDescriptions(): List<String> {
        return medicineList.map { it.getFullDescription() }
    }
}