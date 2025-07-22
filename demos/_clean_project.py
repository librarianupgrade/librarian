import os
import json
import shutil
import cpu_heater
import subprocess

CURRENT_DIR = os.path.dirname(__file__)
BAN_DIR = ['.vscode', '.idea', '.git']


def get_client_projects() -> [str]:
    res: [str] = []
    for path in os.listdir(CURRENT_DIR):
        if path in BAN_DIR:
            continue
        project_path = os.path.join(CURRENT_DIR, path)
        if os.path.isdir(project_path):
            res.append(project_path)
    return res


def switch_jdk(jdk_version: str):
    command = {
        '1.8': 'jenv global 1.8',
        '11': 'jenv global 11',
        '17': 'jenv global 17',
        '23': 'jenv global 23'
    }

    command_str = command.get(jdk_version.strip())

    if command_str is None:
        raise Exception(f"{jdk_version}'s JDK version is not valid.")

    os.system(command_str)


def clean_useless_file_for_project(project_dir: str):
    metadata_path = os.path.join(project_dir, 'GT_Metadata.json')
    with open(metadata_path, 'r') as f:
        metadata = json.load(f)
    idea_dir = os.path.join(project_dir, '.idea')
    if os.path.exists(idea_dir):
        shutil.rmtree(idea_dir)
    vscode_dir = os.path.join(project_dir, '.vscode')
    if os.path.exists(vscode_dir):
        shutil.rmtree(vscode_dir)

    maven_project_path = os.path.join(project_dir, metadata['root_path'])
    result = subprocess.run(['mvn', 'clean'], capture_output=True, text=True,
                            cwd=maven_project_path)

    if 'BUILD SUCCESS' in result.stdout:
        pass
    else:
        raise Exception(f"Maven clean failed for project {project_dir}.")


def clean_dependency_cache_directory(project_dir):
    for root, dirs, files in os.walk(project_dir, topdown=True):
        to_remove = [d for d in dirs if d in {
            "dependency", "after-dependency", "before-dependency"
        }]
        for d in to_remove:
            dir_path = os.path.join(root, d)
            try:
                shutil.rmtree(dir_path)
                print(f"Deleted directory: {dir_path}")
            except Exception as e:
                print(f"Failed to delete {dir_path}: {e}")
            dirs.remove(d)


def classify(client_projects):
    res: dict = {
        '1.8': [],
        '11': [],
        '17': [],
        '23': []
    }
    for project_path in client_projects:
        metadata_file_path = os.path.join(project_path, 'GT_Metadata.json')
        assert os.path.exists(metadata_file_path)
        with open(metadata_file_path, 'r') as f:
            metadata: dict = json.load(f)
        res[metadata.get('jdk_version')].append(project_path)
    return res


def main():
    client_projects = get_client_projects()

    jdk_map = classify(client_projects)
    for jdk_version, project_path_list in jdk_map.items():
        switch_jdk(jdk_version)
        args = [(project_path,) for project_path in project_path_list]
        cpu_heater.multiprocess(clean_useless_file_for_project, args, max_workers=20, show_progress=True)
        cpu_heater.multiprocess(clean_dependency_cache_directory, args, max_workers=20, show_progress=True)
    print('Clean Success.')


if __name__ == '__main__':
    main()
