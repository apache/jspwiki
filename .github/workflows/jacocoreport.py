import xml.etree.ElementTree as ET
import os
  
try:
  # Initialize counters
  total_missed = 0
  total_covered = 0
  # Parse the XML safely
  tree = ET.parse('$REPORT_PATH')
  root = tree.getroot()
      
  # Strategy: Find every <package> node, and grab its immediate <counter type='LINE'> child.
  # This bypasses method/class duplication and scales perfectly to 90k+ lines.
  for package in root.findall('.//package'):
    line_counter = package.find(\"./counter[@type='LINE']\")
      if line_counter is not None:
        total_missed += int(line_counter.get('missed', 0))
        total_covered += int(line_counter.get('covered', 0))
    total_lines = total_missed + total_covered
    if total_lines == 0:
      print('::error::Calculated 0 total lines. Checking alternative fallback layout...')
      # Fallback: If no package-level counters, look for root-level direct children counters
      for counter in root.findall(\"./counter[@type='LINE']\"):
        total_missed = int(counter.get('missed', 0))
        total_covered = int(counter.get('covered', 0))
        total_lines = total_missed + total_covered

    percentage = round((total_covered / total_lines) * 100, 2) if total_lines > 0 else 0.0
      
    # Write directly to the GitHub step summary file
    summary_file = os.environ.get('GITHUB_STEP_SUMMARY')
    with open(summary_file, 'a', encoding='utf-8') as f:
      f.write('### 📊 Precision Project Code Coverage Summary\n\n')
      f.write('| Metric | Value |\n')
      f.write('| --- | --- |\n')
      f.write(f'| **Total Project Lines** | {total_lines:,} |\n')
      f.write(f'| **Total Covered Lines** | {total_covered:,} |\n')
      f.write(f'| **Total Missed Lines** | {total_missed:,} |\n')
      f.write(f'| **Overall Line Coverage** | **{percentage}%** |\n')
          
    print(f'Successfully processed report. Total lines matched: {total_lines:,}')
      
      # Enforce threshold gate natively
    if percentage < 80.0:
      print(f'::error::Overall project coverage ({percentage}%) is below required threshold of 80%!')
      exit(1)
          
except Exception as e:
  print(f'::error::Failed to parse custom JaCoCo XML: {str(e)}')
  exit(1)