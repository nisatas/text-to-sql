const API_BASE = "http://localhost:8080";

const EXAMPLE_QUESTIONS = [
    "12-A sınıfındaki öğrenciler",
    "Matematik notu 50 altı öğrenciler",
    "sınıfların en başarılı öğrencileri",
    "Her dersin ortalaması",
    "Sayısal sınıflardaki öğrenciler",
    "10-A sınıfındaki öğrenciler",
    "Fizikte en yüksek not alanlar",
    "12-B sınıfının notları",
    "En düşük matematik notları",
];

const CLASS_OPTIONS = [
    { value: "", label: "Tümü / belirtme" },
    { value: "grade9", label: "9. sınıflar" },
    { value: "9-A", label: "9-A" },
    { value: "9-B", label: "9-B" },
    { value: "10-A", label: "10-A" },
    { value: "10-B", label: "10-B" },
    { value: "12-A", label: "12-A" },
    { value: "12-B", label: "12-B" },
];

const SUBJECT_OPTIONS = [
    { value: "", label: "Tüm dersler" },
    { value: "Matematik", label: "Matematik" },
    { value: "Fizik", label: "Fizik" },
    { value: "Kimya", label: "Kimya" },
    { value: "Biyoloji", label: "Biyoloji" },
    { value: "Türkçe", label: "Türkçe" },
];

const OPERATIONS = [
    { value: "list_students", label: "Öğrencileri listele" },
    { value: "grades", label: "Notları getir" },
    { value: "top", label: "En başarılıları göster" },
    { value: "fail", label: "Başarısızları göster (50 altı)" },
    { value: "avg", label: "Ortalamayı getir" },
];

function escapeHtml(value) {
    if (value === null || value === undefined) return "";
    const d = document.createElement("div");
    d.textContent = String(value);
    return d.innerHTML;
}

function fillSelect(selectId, options) {
    const el = document.getElementById(selectId);
    el.innerHTML = "";
    for (const o of options) {
        const opt = document.createElement("option");
        opt.value = o.value;
        opt.textContent = o.label;
        el.appendChild(opt);
    }
}

function buildQuestionFromFilters(classValue, subject, op) {
    const isG9 = classValue === "grade9";
    const hasClass = Boolean(classValue && !isG9);
    const subj = subject ? subject.toLowerCase() : "";

    switch (op) {
        case "list_students":
            if (isG9) return "9. sınıflardaki öğrenciler";
            if (hasClass) return `${classValue} sınıfındaki öğrenciler`;
            return "tüm öğrencileri listele";

        case "grades":
            if (isG9 && subj) return `9. sınıfların ${subj} notları`;
            if (isG9) return "9. sınıfların notları";
            if (hasClass && subj) return `${classValue} sınıfının ${subj} notları`;
            if (hasClass) return `${classValue} sınıfının notları`;
            if (subj) return `${subj} dersi notları`;
            return "tüm notları listele";

        case "top":
            if (isG9 && subj) return `9. sınıfların en başarılı ${subj} öğrencileri`;
            if (isG9) return "9. sınıfların en başarılı öğrencileri";
            if (hasClass && subj) return `${classValue} sınıfındaki en başarılı ${subj} öğrencileri`;
            if (hasClass) return `${classValue} sınıfındaki en başarılı öğrenciler`;
            if (subj) return `${subj} dersinde en başarılı öğrenciler`;
            return "sınıfların en başarılı öğrencileri";

        case "fail":
            if (hasClass && subj) return `${classValue} sınıfındaki ${subj} notu 50 altı öğrenciler`;
            if (hasClass) return `${classValue} sınıfındaki notu 50 altı öğrenciler`;
            if (isG9 && subj) return `9. sınıflardaki ${subj} notu 50 altı öğrenciler`;
            if (subj) return `${subj} notu 50 altı öğrenciler`;
            return "notu 50 altı öğrenciler";

        case "avg":
            if (subj) return `${subj} dersinin ortalaması`;
            return "Her dersin ortalaması";

        default:
            return "öğrenci ve not bilgileri";
    }
}

function renderExampleChips() {
    const container = document.getElementById("example-chips");
    for (const q of EXAMPLE_QUESTIONS) {
        const b = document.createElement("button");
        b.type = "button";
        b.className = "chip";
        b.textContent = q;
        b.addEventListener("click", () => {
            document.getElementById("question").value = q;
            sendQuery(q);
        });
        container.appendChild(b);
    }
}

function renderTable(columns, rows) {
    const thead = document.getElementById("result-thead");
    const tbody = document.getElementById("result-tbody");
    thead.innerHTML = "";
    tbody.innerHTML = "";

    if (!columns || columns.length === 0) {
        tbody.innerHTML =
            '<tr><td colspan="99" class="empty-cell">Gösterilecek kolon yok.</td></tr>';
        return;
    }

    const hr = document.createElement("tr");
    for (const c of columns) {
        const th = document.createElement("th");
        th.textContent = c;
        hr.appendChild(th);
    }
    thead.appendChild(hr);

    for (const row of rows || []) {
        const tr = document.createElement("tr");
        for (const cell of row) {
            const td = document.createElement("td");
            td.innerHTML = escapeHtml(cell);
            tr.appendChild(td);
        }
        tbody.appendChild(tr);
    }

    if (!rows || rows.length === 0) {
        const tr = document.createElement("tr");
        const td = document.createElement("td");
        td.colSpan = columns.length;
        td.className = "empty-cell";
        td.textContent = "Kayıt bulunamadı.";
        tr.appendChild(td);
        tbody.appendChild(tr);
    }
}

function showLoading(show) {
    document.getElementById("loading").hidden = !show;
}

function showSuccessBlock(show) {
    document.getElementById("result-success").hidden = !show;
}

function showErrorBlock(show) {
    document.getElementById("result-error").hidden = !show;
}

let lastApiPayload = null;
/** Son başarılı cevabın düz metin özeti (PDF için) */
let lastPlainTextForExport = "";

function cellToString(v) {
    if (v === null || v === undefined) return "";
    return String(v);
}

/**
 * API yanıtından WhatsApp tarzı düz metin üretir.
 */
function formatPlainTextReply(question, data) {
    const cols = data.columns || [];
    const rows = data.rows || [];
    const n = rows.length;

    let out = "";
    out += `Sorduğun şey şuydu: «${question}»\n\n`;
    out += n === 0
        ? "Bu sorguya uygun kayıt bulamadım.\n"
        : `Toplam ${n} satır buldum.\n\n`;

    if (n > 0 && cols.length > 0) {
        rows.forEach((row, idx) => {
            out += `— ${idx + 1} —\n`;
            cols.forEach((c, j) => {
                out += `${c}: ${cellToString(row[j])}\n`;
            });
            out += "\n";
        });
    }

    out += "\nİstersen aşağıdan PDF veya Excel ile de indirebilirsin.";
    return out.trim();
}

function formatPlainTextError(question, data) {
    const err = friendlyErrorMessage(data);
    return (
        `Sorduğun şey şuydu: «${question}»\n\n` +
        `Bu sefer cevap üretemedim. Sebep: ${err}\n\n` +
        `Soruyu biraz değiştirip tekrar dene.`
    );
}

function appendChatBubble(text, role) {
    const thread = document.getElementById("chat-thread");
    const row = document.createElement("div");
    row.className = `chat-row ${role}`;
    const bubble = document.createElement("div");
    bubble.className = "chat-bubble";
    bubble.textContent = text;
    row.appendChild(bubble);
    thread.appendChild(row);
    thread.scrollTop = thread.scrollHeight;
}

function setExportBarVisible(show) {
    const bar = document.getElementById("export-bar");
    bar.hidden = !show;
}

function downloadExcelFromPayload() {
    if (!lastApiPayload || lastApiPayload.status !== "success") return;
    if (typeof XLSX === "undefined") {
        alert("Excel kütüphanesi yüklenemedi. İnternet bağlantını kontrol et.");
        return;
    }
    const cols = lastApiPayload.columns || [];
    const rows = lastApiPayload.rows || [];
    const aoa = [cols, ...rows.map((r) => r.map((c) => c))];
    const ws = XLSX.utils.aoa_to_sheet(aoa);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, "Sonuc");
    const fname = `sorgu-sonuc-${Date.now()}.xlsx`;
    XLSX.writeFile(wb, fname);
}

function downloadPdfFromPlainText() {
    const text = lastPlainTextForExport || "";
    if (!text.trim()) {
        alert("Önce başarılı bir sorgu çalıştır.");
        return;
    }
    if (typeof html2pdf === "undefined") {
        alert("PDF kütüphanesi yüklenemedi. İnternet bağlantını kontrol et.");
        return;
    }
    const el = document.createElement("div");
    el.style.padding = "18px";
    el.style.fontFamily = 'system-ui, "Segoe UI", sans-serif';
    el.style.fontSize = "11px";
    el.style.lineHeight = "1.45";
    el.style.whiteSpace = "pre-wrap";
    el.style.wordBreak = "break-word";
    el.style.color = "#111";
    el.style.background = "#fff";
    el.textContent = text;
    document.body.appendChild(el);

    const opt = {
        margin: 12,
        filename: `sorgu-sonuc-${Date.now()}.pdf`,
        image: { type: "jpeg", quality: 0.95 },
        html2canvas: { scale: 2, useCORS: true },
        jsPDF: { unit: "mm", format: "a4", orientation: "portrait" },
    };

    html2pdf()
        .set(opt)
        .from(el)
        .save()
        .then(() => el.remove())
        .catch(() => {
            el.remove();
            alert("PDF oluşturulamadı.");
        });
}

function updateDevPanel(data) {
    const dev = document.getElementById("result-dev");
    const devMode = document.getElementById("dev-mode").checked;
    const payload = data !== undefined ? data : lastApiPayload;
    const hasTech =
        devMode &&
        payload &&
        (payload.sql || (payload.debug && Object.keys(payload.debug).length));

    if (!devMode) {
        dev.hidden = true;
        return;
    }

    dev.hidden = !hasTech;
    document.getElementById("dev-sql").textContent = payload && payload.sql
        ? `SQL:\n${payload.sql}`
        : "";
    document.getElementById("dev-debug").textContent =
        payload && payload.debug
            ? `Debug:\n${JSON.stringify(payload.debug, null, 2)}`
            : "";
}

function friendlyErrorMessage(data) {
    if (data && data.error && String(data.error).trim()) {
        return data.error;
    }
    return "İşlem tamamlanamadı. Lütfen sorunuzu değiştirip yeniden deneyin.";
}

async function sendQuery(question) {
    const q = (question ?? document.getElementById("question").value).trim();
    if (!q) {
        alert("Lütfen bir soru girin.");
        return;
    }

    appendChatBubble(q, "user");

    showLoading(true);
    showSuccessBlock(false);
    showErrorBlock(false);
    setExportBarVisible(false);
    lastPlainTextForExport = "";
    updateDevPanel(null);

    try {
        const response = await fetch(`${API_BASE}/api/query`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ question: q }),
        });

        const data = await response.json();
        lastApiPayload = data;

        if (data.status === "success") {
            showErrorBlock(false);
            showSuccessBlock(true);
            const n = (data.rows && data.rows.length) || 0;
            document.getElementById("row-count").textContent = `Toplam ${n} satır.`;
            renderTable(data.columns, data.rows);
            lastPlainTextForExport = formatPlainTextReply(q, data);
            appendChatBubble(lastPlainTextForExport, "bot");
            setExportBarVisible(true);
            updateDevPanel(data);
        } else {
            showSuccessBlock(false);
            showErrorBlock(true);
            document.getElementById("error-user-message").textContent =
                friendlyErrorMessage(data);
            appendChatBubble(formatPlainTextError(q, data), "bot");
            setExportBarVisible(false);
            updateDevPanel(data);
        }
    } catch (err) {
        console.error(err);
        lastApiPayload = { sql: null, debug: { networkError: String(err) } };
        showSuccessBlock(false);
        showErrorBlock(true);
        document.getElementById("error-user-message").textContent =
            "Sunucuya bağlanılamadı. Backend çalışıyor mu kontrol edin.";
        appendChatBubble(
            formatPlainTextError(q, {
                status: "error",
                error:
                    "Sunucuya bağlanılamadı. Backend (localhost:8080) çalışıyor mu?",
            }),
            "bot"
        );
        setExportBarVisible(false);
        updateDevPanel(lastApiPayload);
    } finally {
        showLoading(false);
    }
}

function init() {
    fillSelect("filter-class", CLASS_OPTIONS);
    fillSelect("filter-subject", SUBJECT_OPTIONS);
    fillSelect("filter-operation", OPERATIONS);
    renderExampleChips();

    appendChatBubble(
        "Merhaba, ben okul veritabanı asistanıyım. Aşağıdan soru yaz veya hazır sorulardan birine tıkla. Cevapları burada düz yazı olarak göreceksin.",
        "system"
    );

    document.getElementById("btn-send").addEventListener("click", () => sendQuery());
    document.getElementById("question").addEventListener("keydown", (e) => {
        if (e.key === "Enter") sendQuery();
    });

    document.getElementById("btn-build").addEventListener("click", () => {
        const classValue = document.getElementById("filter-class").value;
        const subject = document.getElementById("filter-subject").value;
        const op = document.getElementById("filter-operation").value;
        const built = buildQuestionFromFilters(classValue, subject, op);
        const preview = document.getElementById("built-question-preview");
        preview.hidden = false;
        preview.textContent = `Üretilen soru: «${built}»`;
        document.getElementById("question").value = built;
        sendQuery(built);
    });

    document.getElementById("dev-mode").addEventListener("change", () => {
        updateDevPanel();
    });

    document.getElementById("btn-test").addEventListener("click", async () => {
        const el = document.getElementById("test-status");
        try {
            const res = await fetch(`${API_BASE}/api/test`);
            el.textContent = await res.text();
        } catch (e) {
            el.textContent = "Bağlantı hatası";
        }
    });

    document.getElementById("btn-export-pdf").addEventListener("click", () => {
        downloadPdfFromPlainText();
    });
    document.getElementById("btn-export-xlsx").addEventListener("click", () => {
        downloadExcelFromPayload();
    });
}

init();
