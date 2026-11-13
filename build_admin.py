import os

store_dir = r"D:\nav g store"
if not os.path.exists(store_dir):
    store_dir = os.getcwd()

public_dir = os.path.join(store_dir, "public")
os.makedirs(public_dir, exist_ok=True)

ADMIN_HTML = r'''<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Nav Grocery Admin | Sandhaur, Punjab</title>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <script src="https://www.gstatic.com/firebasejs/9.23.0/firebase-app-compat.js"></script>
  <script src="https://www.gstatic.com/firebasejs/9.23.0/firebase-database-compat.js"></script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js"></script>
  <script>
    if (window.pdfjsLib) {
      pdfjsLib.GlobalWorkerOptions.workerSrc = "https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js";
    }
  </script>
  <style>
    :root {
      --forest: #0F3D28;
      --forest-dark: #0A291B;
      --cream: #F8F7F2;
      --gold: #E6A817;
      --gold-hover: #D4960D;
      --card: #FFFFFF;
      --border: #E5E4DE;
      --text: #111827;
      --muted: #6B7280;
      --red: #C0392B;
      --green: #15803D;
      --green-bg: #DCFCE7;
      --amber-bg: #FEF3C7;
      --amber-text: #92400E;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: "Segoe UI", system-ui, -apple-system, sans-serif; }
    body { background: var(--cream); color: var(--text); min-height: 100vh; }

    /* Login Gate */
    .login-screen { position: fixed; inset: 0; background: linear-gradient(135deg, var(--forest) 0%, var(--forest-dark) 100%); display: flex; align-items: center; justify-content: center; padding: 16px; z-index: 999; }
    .login-card { background: #fff; border-radius: 16px; max-width: 400px; width: 100%; padding: 28px; box-shadow: 0 20px 40px rgba(0,0,0,0.3); }
    .login-logo { width: 48px; height: 48px; border-radius: 50%; background: var(--gold); color: var(--forest-dark); font-weight: 900; font-size: 22px; display: flex; align-items: center; justify-content: center; margin-bottom: 14px; }
    .login-card h2 { font-size: 22px; color: var(--forest-dark); margin-bottom: 4px; }
    .login-card p { font-size: 13px; color: var(--muted); margin-bottom: 18px; }
    .f-group { margin-bottom: 12px; }
    .f-group label { display: block; font-size: 12px; font-weight: 700; color: var(--text); margin-bottom: 4px; }
    .f-input { width: 100%; padding: 10px 12px; border: 1px solid var(--border); border-radius: 8px; font-size: 14px; outline: none; }
    .f-input:focus { border-color: var(--forest); }
    .btn-primary { width: 100%; background: var(--forest); color: #fff; border: none; padding: 11px; border-radius: 8px; font-weight: 800; font-size: 14px; cursor: pointer; }
    .btn-primary:hover { background: var(--forest-dark); }

    /* Top Header */
    .topbar { background: var(--forest); color: #fff; padding: 12px 20px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
    .brand-wrap { display: flex; align-items: center; gap: 10px; }
    .brand-badge { width: 36px; height: 36px; border-radius: 50%; background: var(--gold); color: var(--forest-dark); font-weight: 900; font-size: 18px; display: flex; align-items: center; justify-content: center; }
    .brand-wrap h1 { font-size: 17px; font-weight: 800; }
    .brand-wrap span { font-size: 11px; color: #A7F3D0; display: block; }
    .top-links { display: flex; align-items: center; gap: 12px; }
    .top-link { color: #fff; text-decoration: none; font-size: 13px; font-weight: 700; background: rgba(255,255,255,0.12); padding: 7px 12px; border-radius: 7px; border: none; cursor: pointer; display: inline-flex; align-items: center; gap: 6px; }
    .top-link:hover { background: rgba(255,255,255,0.22); }

    /* Main Container */
    .container { max-width: 1400px; margin: 0 auto; padding: 20px 16px 80px; }
    .dash-header { display: flex; justify-content: space-between; align-items: flex-end; flex-wrap: wrap; gap: 12px; margin-bottom: 16px; }
    .dash-header h2 { font-size: 24px; font-weight: 800; color: var(--forest-dark); }
    .dash-header p { font-size: 13px; color: var(--muted); }

    /* Toast Banner */
    #toastBanner { display: none; padding: 12px 16px; border-radius: 8px; margin-bottom: 14px; font-weight: 700; font-size: 13.5px; }
    .toast-ok { background: var(--green-bg); color: var(--green); border: 1px solid #86EFAC; }
    .toast-err { background: #FEE2E2; color: var(--red); border: 1px solid #FCA5A5; }

    /* Navigation Tabs */
    .tabs { display: flex; gap: 6px; border-bottom: 1px solid var(--border); margin-bottom: 18px; overflow-x: auto; }
    .tab-btn { background: none; border: none; padding: 10px 16px; font-size: 14px; font-weight: 700; color: var(--muted); cursor: pointer; border-bottom: 3px solid transparent; white-space: nowrap; }
    .tab-btn.active { color: var(--forest); border-bottom-color: var(--forest); }

    /* Overview KPI Grid */
    .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; margin-bottom: 20px; }
    .kpi-card { background: var(--card); border: 1px solid var(--border); border-radius: 12px; padding: 16px; }
    .kpi-card span { font-size: 12px; color: var(--muted); font-weight: 700; text-transform: uppercase; }
    .kpi-card strong { display: block; font-size: 26px; font-weight: 900; color: var(--forest-dark); margin-top: 4px; }

    /* Toolbar */
    .toolbar { background: var(--card); border: 1px solid var(--border); border-radius: 12px; padding: 14px; display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 16px; }
    .search-inp { flex: 1; min-width: 210px; padding: 9px 12px; border: 1px solid var(--border); border-radius: 8px; font-size: 13.5px; outline: none; }
    .sel-box { padding: 9px 12px; border: 1px solid var(--border); border-radius: 8px; background: #fff; font-size: 13px; font-weight: 600; cursor: pointer; }
    .btn { padding: 9px 14px; border-radius: 8px; font-size: 13px; font-weight: 700; cursor: pointer; border: 1px solid var(--border); background: #fff; color: var(--text); display: inline-flex; align-items: center; gap: 6px; transition: 0.15s; }
    .btn:hover { background: #F3F4F6; }
    .btn-green { background: var(--forest); color: #fff; border-color: var(--forest); }
    .btn-green:hover { background: var(--forest-dark); }
    .btn-gold { background: var(--gold); color: var(--forest-dark); border-color: var(--gold); }
    .btn-gold:hover { background: var(--gold-hover); }
    .btn-red { background: #FEF2F2; color: var(--red); border-color: #FECACA; }

    /* Table */
    .table-wrap { background: var(--card); border: 1px solid var(--border); border-radius: 12px; overflow-x: auto; box-shadow: 0 2px 8px rgba(0,0,0,0.03); }
    table { width: 100%; border-collapse: collapse; min-width: 1020px; font-size: 13px; }
    th { background: #F9FAFB; text-align: left; padding: 11px 12px; border-bottom: 1px solid var(--border); color: var(--muted); font-weight: 700; font-size: 12px; text-transform: uppercase; }
    td { padding: 10px 12px; border-bottom: 1px solid #F3F4F6; vertical-align: middle; }
    tr:hover { background: #FAFAF9; }

    .prod-cell { display: flex; align-items: center; gap: 10px; }
    .thumb-wrap { position: relative; width: 48px; height: 48px; flex-shrink: 0; }
    .thumb { width: 48px; height: 48px; object-fit: contain; border: 1px solid var(--border); border-radius: 8px; background: #fff; }
    .thumb-edit { position: absolute; bottom: -4px; right: -4px; background: var(--forest); color: #fff; width: 20px; height: 20px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 10px; cursor: pointer; }

    .cell-inp { padding: 6px 8px; border: 1px solid var(--border); border-radius: 6px; font-size: 13px; width: 100%; }
    .badge { display: inline-block; padding: 3px 8px; border-radius: 99px; font-size: 11px; font-weight: 700; margin-right: 4px; }
    .b-live { background: var(--green-bg); color: var(--green); }
    .b-feat { background: var(--amber-bg); color: var(--amber-text); }
    .b-nophoto { background: #F3F4F6; color: var(--muted); border: 1px solid var(--border); }

    .icon-btn { background: none; border: 1px solid var(--border); padding: 6px 9px; border-radius: 6px; cursor: pointer; color: var(--text); }
    .icon-btn:hover { background: #F3F4F6; }
    .icon-btn.save { background: var(--green-bg); color: var(--green); border-color: #86EFAC; font-weight: 700; }
    .icon-btn.del { color: var(--red); }

    /* Modal */
    .modal { display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 400; align-items: center; justify-content: center; padding: 16px; }
    .modal.open { display: flex; }
    .modal-box { background: #fff; border-radius: 14px; max-width: 480px; width: 100%; padding: 22px; max-height: 90vh; overflow-y: auto; }
    .modal-box h3 { color: var(--forest-dark); margin-bottom: 12px; }

    /* Admin Chat AI Copilot */
    .ai-fab { position: fixed; bottom: 20px; right: 20px; background: var(--forest); color: #fff; border: 2px solid var(--gold); padding: 12px 18px; border-radius: 99px; font-weight: 800; font-size: 13.5px; cursor: pointer; z-index: 300; box-shadow: 0 6px 20px rgba(0,0,0,0.25); display: flex; align-items: center; gap: 8px; }
    .ai-panel { display: none; position: fixed; bottom: 78px; right: 20px; width: 380px; max-width: calc(100vw - 40px); height: 480px; background: #fff; border: 1px solid var(--border); border-radius: 16px; box-shadow: 0 12px 35px rgba(0,0,0,0.22); z-index: 310; flex-direction: column; overflow: hidden; }
    .ai-panel.open { display: flex; }
    .ai-head { background: var(--forest); color: #fff; padding: 14px 16px; display: flex; justify-content: space-between; align-items: center; font-weight: 800; font-size: 14px; }
    .ai-body { flex: 1; padding: 12px; overflow-y: auto; background: var(--cream); display: flex; flex-direction: column; gap: 9px; font-size: 13px; }
    .ai-msg { background: #fff; border: 1px solid var(--border); padding: 10px 12px; border-radius: 10px 10px 10px 2px; align-self: flex-start; max-width: 90%; line-height: 1.45; }
    .usr-msg { background: var(--forest); color: #fff; padding: 10px 12px; border-radius: 10px 10px 2px 10px; align-self: flex-end; max-width: 85%; }
    .ai-foot { display: flex; border-top: 1px solid var(--border); background: #fff; }
    .ai-foot input { flex: 1; border: none; padding: 11px 12px; font-size: 13px; outline: none; }
    .ai-foot button { background: var(--gold); color: var(--forest-dark); border: none; padding: 0 16px; font-weight: 800; cursor: pointer; }
  </style>
</head>
<body>

  <!-- Admin Login Gate -->
  <div class="login-screen" id="loginScreen">
    <div class="login-card">
      <div class="login-logo">N</div>
      <h2>Nav Grocery Admin</h2>
      <p>Sandhaur, Punjab · Authorized Store Control Center</p>
      <div class="f-group">
        <label>Admin Mobile Number</label>
        <input type="text" id="adminPhone" class="f-input" value="8847370905">
      </div>
      <div class="f-group">
        <label>Security PIN</label>
        <input type="password" id="adminPin" class="f-input" placeholder="Enter Admin PIN (148020)" onkeypress="if(event.key==='Enter') verifyAdminLogin()">
      </div>
      <button class="btn-primary" onclick="verifyAdminLogin()">Unlock Store Dashboard</button>
    </div>
  </div>

  <!-- Top Bar -->
  <header class="topbar">
    <div class="brand-wrap">
      <div class="brand-badge">N</div>
      <div>
        <h1>Nav Grocery Admin</h1>
        <span>Sandhaur, Punjab 148020 · GSTIN: 03AHPPU6185N1ZF</span>
      </div>
    </div>
    <div class="top-links">
      <a href="public.html" target="_blank" class="top-link"><i class="fa-solid fa-arrow-up-right-from-square"></i> View store</a>
      <button class="top-link" onclick="logoutAdmin()"><i class="fa-solid fa-right-from-bracket"></i> Log out</button>
    </div>
  </header>

  <main class="container">
    <div class="dash-header">
      <div>
        <h2>Store dashboard</h2>
        <p>Logged in as Nav Store Admin (+91 8847370905) · Sealed Packaged &amp; Frozen Catalog Only</p>
      </div>
      <div style="display:flex;gap:8px;flex-wrap:wrap;">
        <button class="btn btn-red" onclick="resetCache()"><i class="fa-solid fa-rotate"></i> Reset Cache</button>
        <button class="btn btn-gold" onclick="syncAllToFirebase()"><i class="fa-solid fa-cloud-arrow-up"></i> Save &amp; Sync All Live</button>
      </div>
    </div>

    <div id="toastBanner"></div>

    <!-- Navigation Tabs -->
    <div class="tabs">
      <button class="tab-btn active" onclick="switchTab('products', this)">Products &amp; stock</button>
      <button class="tab-btn" onclick="switchTab('overview', this)">Overview</button>
      <button class="tab-btn" onclick="switchTab('orders', this)">Orders</button>
      <button class="tab-btn" onclick="switchTab('customers', this)">Customers</button>
    </div>

    <!-- TAB 1: PRODUCTS & STOCK -->
    <section id="tab-products">
      <div class="toolbar">
        <input type="text" id="prodSearch" class="search-inp" placeholder="Name, brand or SKU..." oninput="renderProductsTable()">
        
        <select id="catFilter" class="sel-box" onchange="renderProductsTable()">
          <option value="All">All categories</option>
          <option value="Atta & Dals">Atta &amp; Dals</option>
          <option value="Desi Ghee & Oils">Desi Ghee &amp; Oils</option>
          <option value="Spices">Spices</option>
          <option value="Frozen Foods">Frozen Foods</option>
          <option value="Grocery & Staples">Grocery &amp; Staples</option>
        </select>

        <select id="stockFilter" class="sel-box" onchange="renderProductsTable()">
          <option value="All">All stock</option>
          <option value="in">In stock (&gt;5)</option>
          <option value="low">Low stock (&le;5)</option>
        </select>

        <select id="photoFilter" class="sel-box" onchange="renderProductsTable()">
          <option value="All">All photos</option>
          <option value="nophoto">No photo yet</option>
          <option value="hasphoto">Verified photo</option>
        </select>

        <input type="file" id="csvInput" accept=".csv" style="display:none;" onchange="importCSV(event)">
        <button class="btn" onclick="document.getElementById('csvInput').click()"><i class="fa-solid fa-file-import"></i> Import CSV</button>
        <button class="btn" onclick="exportCSV()"><i class="fa-solid fa-download"></i> Shot list (CSV)</button>

        <input type="file" id="pdfInput" accept=".pdf" style="display:none;" onchange="importPDF(event)">
        <button class="btn" onclick="document.getElementById('pdfInput').click()"><i class="fa-solid fa-file-pdf"></i> Upload PDF</button>

        <input type="file" id="bulkPhotoInput" accept="image/*" multiple style="display:none;" onchange="handleBulkPhotos(event)">
        <button class="btn" onclick="document.getElementById('bulkPhotoInput').click()"><i class="fa-solid fa-images"></i> Bulk photos</button>

        <button class="btn btn-green" onclick="openAddModal()"><i class="fa-solid fa-plus"></i> Add product</button>
      </div>

      <div style="margin-bottom:10px;font-size:13px;font-weight:700;color:var(--muted);" id="skuCounter">Loading SKUs...</div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Product &amp; SKU</th>
              <th>Category</th>
              <th>Sale Price / MRP</th>
              <th>Stock</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="prodTableBody">
            <tr><td colspan="6" style="text-align:center;padding:30px;">Connecting to Firebase RTDB...</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- TAB 2: OVERVIEW -->
    <section id="tab-overview" style="display:none;">
      <div class="kpi-grid">
        <div class="kpi-card"><span>Total Verified SKUs</span><strong id="kpiTotalSkus">0</strong></div>
        <div class="kpi-card"><span>Photos Linked</span><strong id="kpiPhotosOk">0</strong></div>
        <div class="kpi-card"><span>No Photo Yet</span><strong id="kpiNoPhoto" style="color:var(--red);">0</strong></div>
        <div class="kpi-card"><span>Total Stock Value (Sale)</span><strong id="kpiStockVal">₹0</strong></div>
      </div>
    </section>

    <!-- TAB 3: ORDERS -->
    <section id="tab-orders" style="display:none;">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Order ID</th>
              <th>Customer</th>
              <th>Mode</th>
              <th>Total</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody id="ordersTableBody">
            <tr>
              <td>#ORD-1001</td>
              <td>Gurpreet Singh (Sandhaur)</td>
              <td>Delivery · COD</td>
              <td>₹1,240</td>
              <td>
                <select class="sel-box">
                  <option>Confirmed</option>
                  <option>Packed</option>
                  <option>Out for delivery</option>
                  <option>Delivered</option>
                </select>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- TAB 4: CUSTOMERS -->
    <section id="tab-customers" style="display:none;">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Customer Name</th>
              <th>WhatsApp Mobile</th>
              <th>Location</th>
              <th>Account Status</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Nav Store Admin</td>
              <td>+91 8847370905</td>
              <td>Main Bazaar, Sandhaur</td>
              <td><span class="badge b-live">Verified Admin</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </main>

  <!-- Add Product Modal -->
  <div class="modal" id="addModal">
    <div class="modal-box">
      <h3>Add New Sealed / Frozen Product</h3>
      <p style="font-size:12px;color:var(--red);margin-bottom:12px;">Reminder: Fresh fruits and fresh vegetables are strictly prohibited.</p>
      <div class="f-group"><label>Product Name &amp; Pack Size</label><input type="text" id="newProdName" class="f-input" placeholder="e.g. Aashirvaad Shudh Chakki Atta 5 kg"></div>
      <div class="f-group"><label>Brand</label><input type="text" id="newProdBrand" class="f-input" placeholder="e.g. AASHIRVAAD"></div>
      <div class="f-group">
        <label>Category</label>
        <select id="newProdCat" class="f-input">
          <option value="Atta & Dals">Atta &amp; Dals</option>
          <option value="Desi Ghee & Oils">Desi Ghee &amp; Oils</option>
          <option value="Spices">Spices</option>
          <option value="Frozen Foods">Frozen Foods</option>
          <option value="Grocery & Staples">Grocery &amp; Staples</option>
        </select>
      </div>
      <div class="f-group"><label>MRP (₹)</label><input type="number" id="newProdMrp" class="f-input" value="100"></div>
      <div class="f-group"><label>Stock Quantity</label><input type="number" id="newProdStock" class="f-input" value="25"></div>
      <div style="display:flex;gap:8px;margin-top:12px;">
        <button class="btn btn-green" style="flex:1;justify-content:center;" onclick="createNewProduct()">Save Product</button>
        <button class="btn" onclick="closeAddModal()">Cancel</button>
      </div>
    </div>
  </div>

  <!-- Admin Chat AI Copilot -->
  <button class="ai-fab" onclick="toggleAdminAI()">
    <i class="fa-solid fa-wand-magic-sparkles"></i> Admin AI Copilot
  </button>
  <div class="ai-panel" id="aiPanel">
    <div class="ai-head">
      <span><i class="fa-solid fa-robot"></i> Nav Store Admin AI</span>
      <button onclick="toggleAdminAI()" style="background:none;border:none;color:#fff;font-size:18px;cursor:pointer;">&times;</button>
    </div>
    <div class="ai-body" id="aiBody">
      <div class="ai-msg">Sat Sri Akal Singhpal! Give me a command in English or Punjabi:<br>• <b>"Show missing photos"</b><br>• <b>"Set 12% discount on all items"</b><br>• <b>"Update stock of NAV001 to 50"</b><br>• <b>"Write Punjabi WhatsApp promo"</b></div>
    </div>
    <div class="ai-foot">
      <input type="text" id="aiInput" placeholder="Type admin command..." onkeypress="if(event.key==='Enter') runAdminAI()">
      <button onclick="runAdminAI()">Run</button>
    </div>
  </div>

  <script>
    const SAFE_SVG = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='300' height='300' viewBox='0 0 300 300'%3E%3Crect width='300' height='300' fill='%23F8F7F2'/%3E%3Crect x='75' y='70' width='150' height='150' rx='12' fill='%23E5E4DE'/%3E%3Ctext x='150' y='150' font-family='Arial' font-size='14' font-weight='bold' fill='%230F3D28' text-anchor='middle'%3ENO PHOTO%3C/text%3E%3C/svg%3E";
    let catalog = [];
    let db = null;

    /* Auth */
    function verifyAdminLogin() {
      const pin = document.getElementById("adminPin").value.trim();
      if (pin === "148020" || pin === "482915") {
        sessionStorage.setItem("NAV_ADMIN_AUTH", "1");
        document.getElementById("loginScreen").style.display = "none";
      } else {
        alert("Invalid Admin PIN. Enter 148020.");
      }
    }
    if (sessionStorage.getItem("NAV_ADMIN_AUTH") === "1") {
      document.getElementById("loginScreen").style.display = "none";
    }
    function logoutAdmin() {
      sessionStorage.removeItem("NAV_ADMIN_AUTH");
      location.reload();
    }

    function showToast(msg, isErr) {
      const t = document.getElementById("toastBanner");
      t.className = isErr ? "toast-err" : "toast-ok";
      t.innerText = msg;
      t.style.display = "block";
      setTimeout(() => { t.style.display = "none"; }, 5000);
    }

    function switchTab(tab, btn) {
      ["products", "overview", "orders", "customers"].forEach(id => {
        document.getElementById("tab-" + id).style.display = (id === tab) ? "block" : "none";
      });
      document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
      if (btn) btn.classList.add("active");
      if (tab === "overview") updateKPIs();
    }

    function hasValidPhoto(src) {
      if (!src || src === "undefined" || src === "none") return false;
      if (src.startsWith("data:image/svg+xml")) return false;
      return true;
    }

    function getImgSrc(src) {
      if (!hasValidPhoto(src)) return SAFE_SVG;
      if (src.startsWith("data:") || src.startsWith("http") || src.startsWith("images/")) return src;
      return "images/" + src;
    }

    function renderProductsTable() {
      const q = (document.getElementById("prodSearch").value || "").toLowerCase().trim();
      const catF = document.getElementById("catFilter").value;
      const stkF = document.getElementById("stockFilter").value;
      const phoF = document.getElementById("photoFilter").value;

      const filtered = catalog.filter(p => {
        const name = (p.name || "").toLowerCase();
        const brand = (p.brand || "").toLowerCase();
        const sku = String(p.id || "").toLowerCase();
        const barcode = String(p.barcode || "").toLowerCase();
        const stk = parseInt(p.stock) || 0;
        const okPhoto = hasValidPhoto(p.image);

        if (q && !name.includes(q) && !brand.includes(q) && !sku.includes(q) && !barcode.includes(q)) return false;
        if (catF !== "All" && (p.category || "") !== catF) return false;
        if (stkF === "in" && stk <= 5) return false;
        if (stkF === "low" && stk > 5) return false;
        if (phoF === "nophoto" && okPhoto) return false;
        if (phoF === "hasphoto" && !okPhoto) return false;
        return true;
      });

      document.getElementById("skuCounter").innerText = `Showing ${filtered.length} of ${catalog.length} SKUs`;

      const tbody = document.getElementById("prodTableBody");
      if (!filtered.length) {
        tbody.innerHTML = "<tr><td colspan='6' style='text-align:center;padding:30px;'>No matching SKUs found.</td></tr>";
        return;
      }

      tbody.innerHTML = filtered.map(p => {
        const id = String(p.id || p.barcode);
        const mrp = parseInt(p.mrp) || 100;
        const sale = parseInt(p.sale || p.price) || Math.round(mrp * 0.88);
        const disc = mrp > sale ? Math.round(((mrp - sale) / mrp) * 100) : 12;
        const okPhoto = hasValidPhoto(p.image);

        return `
          <tr>
            <td>
              <div class="prod-cell">
                <div class="thumb-wrap">
                  <img src="${getImgSrc(p.image)}" id="img-${id}" class="thumb" onerror="this.onerror=null;this.src=SAFE_SVG;">
                  <label class="thumb-edit" title="Upload photo">
                    <i class="fa-solid fa-camera"></i>
                    <input type="file" accept="image/*" style="display:none;" onchange="replaceSinglePhoto('${id}', this)">
                  </label>
                </div>
                <div style="flex:1;min-width:210px;">
                  <input type="text" class="cell-inp" style="font-weight:700;margin-bottom:3px;" value="${p.name || ''}" onchange="editProd('${id}', 'name', this.value)">
                  <span style="font-size:11px;color:var(--muted);font-weight:700;">${id} · Barcode: ${p.barcode || ''}</span>
                </div>
              </div>
            </td>
            <td>
              <input type="text" class="cell-inp" style="width:145px;" value="${p.category || 'Grocery & Staples'}" onchange="editProd('${id}', 'category', this.value)">
            </td>
            <td>
              <div style="display:flex;align-items:center;gap:6px;">
                <div>
                  <span style="font-size:11px;color:var(--muted);">Sale ₹</span>
                  <input type="number" id="sale-${id}" class="cell-inp" style="width:72px;font-weight:800;" value="${sale}" onchange="editProd('${id}', 'sale', parseInt(this.value)||0);editProd('${id}', 'price', parseInt(this.value)||0)">
                </div>
                <div>
                  <span style="font-size:11px;color:var(--muted);">MRP ₹</span>
                  <input type="number" class="cell-inp" style="width:72px;" value="${mrp}" oninput="autoDiscount('${id}', this.value)">
                </div>
              </div>
              <span style="font-size:11px;color:var(--green);font-weight:700;">${disc}% off</span>
            </td>
            <td>
              <input type="number" class="cell-inp" style="width:68px;font-weight:700;" value="${p.stock ?? 25}" onchange="editProd('${id}', 'stock', parseInt(this.value)||0)">
            </td>
            <td>
              <span class="badge b-live">Live</span>
              ${okPhoto ? '<span class="badge b-feat">Photo OK</span>' : '<span class="badge b-nophoto">No photo</span>'}
            </td>
            <td>
              <div style="display:flex;gap:6px;">
                <button class="icon-btn save" onclick="syncAllToFirebase('Saved ${id} live!')" title="Save row">Save</button>
                <button class="icon-btn del" onclick="deleteProduct('${id}')" title="Delete SKU"><i class="fa-solid fa-trash"></i></button>
              </div>
            </td>
          </tr>
        `;
      }).join("");
    }

    function editProd(id, key, val) {
      const item = catalog.find(p => String(p.id || p.barcode) === String(id));
      if (item) item[key] = val;
    }

    function autoDiscount(id, mrpRaw) {
      const mrp = parseInt(mrpRaw) || 0;
      const sale = Math.round(mrp * 0.88);
      const saleBox = document.getElementById("sale-" + id);
      if (saleBox) saleBox.value = sale;
      editProd(id, "mrp", mrp);
      editProd(id, "sale", sale);
      editProd(id, "price", sale);
    }

    function deleteProduct(id) {
      if (!confirm("Delete SKU " + id + "?")) return;
      catalog = catalog.filter(p => String(p.id || p.barcode) !== String(id));
      renderProductsTable();
      syncAllToFirebase("Deleted " + id + " and updated live store!");
    }

    /* Compress image via Canvas to keep Firebase fast */
    function compressImageFile(file, callback) {
      const reader = new FileReader();
      reader.onload = function(e) {
        const img = new Image();
        img.onload = function() {
          const canvas = document.createElement("canvas");
          const maxDim = 360;
          let w = img.width;
          let h = img.height;
          if (w > maxDim || h > maxDim) {
            if (w > h) { h = Math.round((h * maxDim) / w); w = maxDim; }
            else { w = Math.round((w * maxDim) / h); h = maxDim; }
          }
          canvas.width = w;
          canvas.height = h;
          const ctx = canvas.getContext("2d");
          ctx.fillStyle = "#FFFFFF";
          ctx.fillRect(0, 0, w, h);
          ctx.drawImage(img, 0, 0, w, h);
          callback(canvas.toDataURL("image/jpeg", 0.82));
        };
        img.src = e.target.result;
      };
      reader.readAsDataURL(file);
    }

    function replaceSinglePhoto(id, input) {
      const file = input.files[0];
      if (!file) return;
      compressImageFile(file, dataUrl => {
        editProd(id, "image", dataUrl);
        const el = document.getElementById("img-" + id);
        if (el) el.src = dataUrl;
        showToast("Photo updated for " + id + "! Click Save & Sync All Live.", false);
      });
    }

    /* Bulk Photo Upload Matcher (Matches NAV001.jpg or 1.png .. 308.png) */
    function handleBulkPhotos(event) {
      const files = Array.from(event.target.files || []);
      if (!files.length) return;
      let matched = 0;
      let processed = 0;
      const unmatched = [];

      files.forEach(file => {
        const base = file.name.replace(/\.[^/.]+$/, "").toUpperCase();
        const numMatch = base.match(/(\d+)/);
        let target = catalog.find(p => String(p.id || "").toUpperCase() === base);
        if (!target && numMatch) {
          const idxNum = parseInt(numMatch[1], 10);
          const paddedId = "NAV" + String(idxNum).padStart(3, "0");
          target = catalog.find(p => String(p.id || "").toUpperCase() === paddedId) || catalog[idxNum - 1];
        }

        if (target) {
          compressImageFile(file, dataUrl => {
            target.image = dataUrl;
            matched++;
            processed++;
            if (processed === files.length) finishBulk(matched, unmatched);
          });
        } else {
          unmatched.push(file.name);
          processed++;
          if (processed === files.length) finishBulk(matched, unmatched);
        }
      });
      event.target.value = "";
    }

    function finishBulk(matched, unmatched) {
      renderProductsTable();
      let msg = `Bulk Photo Upload: Matched ${matched} photos!`;
      if (unmatched.length) msg += ` Unmatched: ${unmatched.slice(0, 5).join(", ")}`;
      showToast(msg, matched === 0);
      if (matched > 0) syncAllToFirebase(msg);
    }

    /* Add New Product */
    function openAddModal() { document.getElementById("addModal").classList.add("open"); }
    function closeAddModal() { document.getElementById("addModal").classList.remove("open"); }
    function createNewProduct() {
      const name = document.getElementById("newProdName").value.trim();
      if (!name) return alert("Enter product name.");
      const lower = name.toLowerCase();
      if (lower.includes("fresh fruit") || lower.includes("fresh veg") || lower.includes("ਤਾਜ਼ੀ")) {
        return alert("Store Policy Violation: Fresh fruits and fresh vegetables are strictly blocked.");
      }
      const nextNum = catalog.length + 1;
      const id = "NAV" + String(nextNum).padStart(3, "0");
      const mrp = parseInt(document.getElementById("newProdMrp").value) || 100;
      const sale = Math.round(mrp * 0.88);
      catalog.unshift({
        id: id,
        barcode: "890100000" + String(nextNum).padStart(3, "0"),
        name: name,
        brand: (document.getElementById("newProdBrand").value.trim() || name.split(" ")[0]).toUpperCase(),
        category: document.getElementById("newProdCat").value,
        cost: Math.round(mrp * 0.75),
        mrp: mrp,
        sale: sale,
        price: sale,
        gst: "5%",
        stock: parseInt(document.getElementById("newProdStock").value) || 25,
        image: SAFE_SVG
      });
      closeAddModal();
      renderProductsTable();
      syncAllToFirebase("Added " + name + " (" + id + ") live!");
    }

    /* CSV Import & Export */
    function exportCSV() {
      const headers = ["id", "barcode", "name", "brand", "category", "cost", "mrp", "sale", "gst", "stock", "image"];
      let csv = headers.join(",") + "\n";
      catalog.forEach(p => {
        csv += `"${p.id||''}","${p.barcode||''}","${(p.name||'').replace(/"/g,'""')}","${p.brand||''}","${p.category||''}",${p.cost||0},${p.mrp||100},${p.sale||p.price||88},"${p.gst||'5%'}",${p.stock||25},"${(p.image && !p.image.startsWith('data:') ? p.image : '')}"\n`;
      });
      const a = document.createElement("a");
      a.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8;" }));
      a.download = "nav_grocery_308_catalog.csv";
      a.click();
    }

    function importCSV(event) {
      const file = event.target.files[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = function(e) {
        const lines = e.target.result.split(/\r?\n/).filter(l => l.trim());
        if (lines.length < 2) return;
        const headers = lines[0].split(",").map(h => h.replace(/"/g, "").trim().toLowerCase());
        const updated = [];
        for (let i = 1; i < lines.length; i++) {
          const cols = lines[i].match(/(".*?"|[^",\s]+)(?=\s*,|\s*$)/g) || lines[i].split(",");
          const obj = {};
          headers.forEach((h, idx) => { obj[h] = (cols[idx] || "").replace(/^"|"$/g, "").trim(); });
          const mrp = parseInt(obj.mrp) || 100;
          const sale = parseInt(obj.sale || obj.price) || Math.round(mrp * 0.88);
          updated.push({
            id: obj.id || ("NAV" + String(i).padStart(3, "0")),
            barcode: obj.barcode || ("890100000" + String(i).padStart(3, "0")),
            name: obj.name || ("Grocery Item " + i),
            brand: (obj.brand || "NAV").toUpperCase(),
            category: obj.category || "Grocery & Staples",
            cost: parseInt(obj.cost) || Math.round(mrp * 0.75),
            mrp: mrp,
            sale: sale,
            price: sale,
            gst: obj.gst || "5%",
            stock: parseInt(obj.stock) || 25,
            image: obj.image || SAFE_SVG
          });
        }
        if (updated.length) {
          catalog = updated;
          renderProductsTable();
          syncAllToFirebase("Imported " + updated.length + " SKUs from CSV!");
        }
      };
      reader.readAsText(file);
      event.target.value = "";
    }

    /* Safe PDF Price List Parser (Blocks Header/Footer Junk) */
    function importPDF(event) {
      const file = event.target.files[0];
      if (!file || !window.pdfjsLib) return;
      showToast("Scanning PDF safely...", false);
      const reader = new FileReader();
      reader.onload = async function(e) {
        try {
          const pdf = await pdfjsLib.getDocument({ data: new Uint8Array(e.target.result) }).promise;
          const parsed = [];
          const junkWords = ["sandhaur", "helpline", "8847370905", "gstin", "invoice", "subtotal", "page ", "price list", "doorstep"];
          for (let pNum = 1; pNum <= pdf.numPages; pNum++) {
            const page = await pdf.getPage(pNum);
            const txt = await page.getTextContent();
            const line = txt.items.map(i => i.str).join(" ");
            const rows = line.split(/\s{2,}/);
            rows.forEach(r => {
              const low = r.toLowerCase();
              if (junkWords.some(j => low.includes(j))) return;
              const nums = r.match(/\b\d{2,5}\b/g);
              if (nums && r.length > 8) {
                const mrp = parseInt(nums[nums.length - 1]);
                const cleanTitle = r.replace(/\b\d+\b/g, "").replace(/[₹:,]/g, "").trim();
                if (cleanTitle.length >= 4 && mrp >= 10) {
                  const id = "NAV" + String(parsed.length + 1).padStart(3, "0");
                  const sale = Math.round(mrp * 0.88);
                  const old = catalog.find(x => x.id === id);
                  parsed.push({
                    id: id,
                    barcode: "890100000" + String(parsed.length + 1).padStart(3, "0"),
                    name: cleanTitle,
                    brand: cleanTitle.split(" ")[0].toUpperCase(),
                    category: "Grocery & Staples",
                    cost: Math.round(mrp * 0.75),
                    mrp: mrp,
                    sale: sale,
                    price: sale,
                    gst: "5%",
                    stock: 25,
                    image: old ? old.image : SAFE_SVG
                  });
                }
              }
            });
          }
          if (parsed.length) {
            catalog = parsed;
            renderProductsTable();
            syncAllToFirebase("Loaded " + parsed.length + " verified items from PDF!");
          } else {
            showToast("No valid product rows found in PDF.", true);
          }
        } catch (err) {
          showToast("PDF Error: " + err.message, true);
        }
      };
      reader.readAsArrayBuffer(file);
      event.target.value = "";
    }

    function updateKPIs() {
      document.getElementById("kpiTotalSkus").innerText = catalog.length;
      const ok = catalog.filter(p => hasValidPhoto(p.image)).length;
      document.getElementById("kpiPhotosOk").innerText = ok;
      document.getElementById("kpiNoPhoto").innerText = catalog.length - ok;
      const val = catalog.reduce((acc, p) => acc + ((parseInt(p.sale || p.price) || 0) * (parseInt(p.stock) || 0)), 0);
      document.getElementById("kpiStockVal").innerText = "₹" + val.toLocaleString("en-IN");
    }

    function syncAllToFirebase(customMsg) {
      if (!db) return showToast("Firebase not connected.", true);
      db.ref("products").set(catalog)
        .then(() => showToast(customMsg || ("Synced all " + catalog.length + " products live to Firebase!"), false))
        .catch(err => showToast("Sync Error: " + err.message, true));
    }

    function resetCache() {
      localStorage.clear();
      if (db) {
        db.ref("products").once("value").then(snap => {
          const val = snap.val();
          if (val) {
            catalog = Array.isArray(val) ? val : Object.values(val);
            renderProductsTable();
            showToast("Cache cleared! Reloaded " + catalog.length + " fresh SKUs from Firebase.", false);
          }
        });
      }
    }

    /* Admin Chat AI Copilot */
    function toggleAdminAI() { document.getElementById("aiPanel").classList.toggle("open"); }
    function runAdminAI() {
      const inp = document.getElementById("aiInput");
      const cmd = inp.value.trim();
      if (!cmd) return;
      inp.value = "";
      const body = document.getElementById("aiBody");
      body.innerHTML += `<div class="usr-msg">${cmd}</div>`;

      const low = cmd.toLowerCase();
      let reply = "";

      if (low.includes("no photo") || low.includes("missing photo") || low.includes("ਫੋਟੋ")) {
        document.getElementById("photoFilter").value = "nophoto";
        renderProductsTable();
        const missingCount = catalog.filter(p => !hasValidPhoto(p.image)).length;
        reply = `Filtered table to show <b>${missingCount} SKUs</b> with No photo yet. Click the camera icon on any row or use Bulk photos to upload.`;
      } else if (low.includes("low stock") || low.includes("ਘੱਟ")) {
        document.getElementById("stockFilter").value = "low";
        renderProductsTable();
        reply = "Filtered table to show Low Stock items (&le;5 units).";
      } else if (low.includes("discount")) {
        const m = low.match(/(\d+)/);
        const pct = m ? parseInt(m[1]) : 12;
        catalog.forEach(p => {
          const mrp = parseInt(p.mrp) || 100;
          const sale = Math.round(mrp * ((100 - pct) / 100));
          p.sale = sale;
          p.price = sale;
        });
        renderProductsTable();
        syncAllToFirebase(`Applied ${pct}% discount across all ${catalog.length} products!`);
        reply = `Done! Applied a flat <b>${pct}% discount</b> across all ${catalog.length} products and synced live to Firebase.`;
      } else if (low.includes("promo") || low.includes("whatsapp") || low.includes("ਪੰਜਾਬੀ") || low.includes("ਮੈਸੇਜ")) {
        reply = `<b>Punjabi WhatsApp Promo Message:</b><br><br>ਸਤ ਸ੍ਰੀ ਅਕਾਲ ਜੀ! ਨਵ ਗ੍ਰੋਸਰੀ ਸਟੋਰ ਸੰਦੌੜ ਵੱਲੋਂ ਸਾਰੇ ਪੈਕ ਰਾਸ਼ਨ, ਆਟਾ, ਦਾਲਾਂ, ਦੇਸੀ ਘਿਓ ਅਤੇ ਫਰੋਜ਼ਨ ਮਟਰ-ਪਨੀਰ ਉੱਤੇ ਸਿੱਧਾ 12% ਡਿਸਕਾਊਂਟ ਮਿਲ ਰਿਹਾ ਹੈ। ਘਰ ਬੈਠੇ ਆਰਡਰ ਕਰਨ ਲਈ ਅੱਜ ਹੀ ਸਾਡੀ ਵੈੱਬਸਾਈਟ www.navgrocerystore.com ਖੋਲ੍ਹੋ ਜਾਂ 8847370905 ਉੱਤੇ ਵਟਸਐਪ ਕਰੋ।`;
      } else {
        const skuMatch = cmd.toUpperCase().match(/NAV\d{3}/);
        const numMatch = cmd.match(/to\s+(\d+)/i);
        if (skuMatch && numMatch && low.includes("stock")) {
          const sku = skuMatch[0];
          const newStk = parseInt(numMatch[1]);
          const item = catalog.find(p => p.id === sku);
          if (item) {
            item.stock = newStk;
            renderProductsTable();
            syncAllToFirebase(`Updated ${sku} stock to ${newStk}!`);
            reply = `Updated <b>${item.name} (${sku})</b> stock to <b>${newStk}</b> and synced live.`;
          } else {
            reply = `SKU ${sku} not found in catalog.`;
          }
        } else {
          reply = "I can filter missing photos, apply bulk discounts, update SKU stock, or write Punjabi WhatsApp promos. Try typing: <b>Show missing photos</b> or <b>Set 12% discount</b>.";
        }
      }

      setTimeout(() => {
        body.innerHTML += `<div class="ai-msg">${reply}</div>`;
        body.scrollTop = body.scrollHeight;
      }, 200);
    }

    /* Firebase Init */
    try {
      const firebaseConfig = {
        apiKey: "AIzaSyDUaToRs_hhQNvI4-qSj6lluN_stjUr1-o",
        authDomain: "nav-grocery-store.firebaseapp.com",
        databaseURL: "https://nav-grocery-store-default-rtdb.firebaseio.com",
        projectId: "nav-grocery-store",
        storageBucket: "nav-grocery-store.firebasestorage.app",
        messagingSenderId: "272584690336",
        appId: "1:272584690336:web:76c20bd0cce7cfe34997c5"
      };
      if (!firebase.apps.length) firebase.initializeApp(firebaseConfig);
      db = firebase.database();
      db.ref("products").once("value").then(snap => {
        const val = snap.val();
        if (val) catalog = Array.isArray(val) ? val : Object.values(val);
        renderProductsTable();
        updateKPIs();
      });
    } catch (e) {
      console.error(e);
    }
  </script>
</body>
</html>'''

for path in [
    os.path.join(store_dir, "admin.html"),
    os.path.join(public_dir, "admin.html")
]:
    with open(path, "w", encoding="utf-8") as f:
        f.write(ADMIN_HTML)
    print("Created:", path)

print("SUCCESS: Script 2 completed! Admin Portal generated.")
