import sys
import os
import xml.etree.ElementTree as ET


def get_coverage(xml_path):
    if not os.path.isfile(xml_path):
        print(f"ERROR: Coverage report not found: {xml_path}")
        sys.exit(1)

    try:
        tree = ET.parse(xml_path)
    except ET.ParseError as e:
        print(f"ERROR: Failed to parse coverage report '{xml_path}': {e}")
        sys.exit(1)

    root = tree.getroot()
    # Get the overall counter (direct child of report element, not nested in packages/classes)
    # Use LINE coverage to match Azure DevOps reporting
    counter = root.find("./counter[@type='LINE']")
    if counter is None:
        print(f"ERROR: No LINE counter found in coverage report: {xml_path}")
        sys.exit(1)

    covered = int(counter.attrib['covered'])
    missed = int(counter.attrib['missed'])
    total = covered + missed
    if total == 0:
        print(f"WARNING: No lines found in coverage report: {xml_path}. Treating as 0% coverage.")
        return 0.0
    return covered / total


def main():
    if len(sys.argv) != 3:
        print(f"Usage: {sys.argv[0]} <pr_coverage_report> <dev_coverage_report>")
        sys.exit(1)

    pr_cov = get_coverage(sys.argv[1])
    dev_cov = get_coverage(sys.argv[2])

    print(f"PR branch coverage: {pr_cov:.2%}")
    print(f"Dev branch coverage: {dev_cov:.2%}")

    if pr_cov < dev_cov:
        print("ERROR: PR branch coverage is lower than dev branch. Failing...")
        sys.exit(1)
    else:
        print("SUCCESS: PR branch coverage is not lower than dev branch, this is acceptable!")
        sys.exit(0)


if __name__ == '__main__':
    main()
