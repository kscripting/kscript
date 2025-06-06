from setuptools import setup

setup(
    name='kscript',
    version='4.2.3',
    author='Holger Brandl, Marcin Kuszczak',
    author_email='holgerbrandl@gmail.com, aarti@interia.pl',
    description='KScript - easy scripting with Kotlin',
    url='https://github.com/kscripting/kscript',
    license='MIT',
    packages=['wrappers'],
    entry_points={
        'console_scripts': [
            'kscript=wrappers.kscript_py_wrapper:main'
        ]
    },
    package_data={
        'wrappers': ['kscript.jar'] # Assume kscript.jar is copied to wrappers directory
    },
    classifiers=[
        'Programming Language :: Python :: 3',
        'License :: OSI Approved :: MIT License',
        'Operating System :: OS Independent',
    ],
    python_requires='>=3.6',
)
